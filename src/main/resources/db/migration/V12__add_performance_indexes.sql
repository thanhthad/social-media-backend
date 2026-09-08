-- =====================================================================================
-- MIGRATION: V12__add_performance_indexes.sql
-- MỤC ĐÍCH:
--   Đánh chỉ mục (Index) toàn diện cho toàn bộ cơ sở dữ liệu dự án Social Network,
--   dựa trên phân tích 38 Spring Data JPA Repository và toàn bộ các câu query JPQL / Native SQL.
--
-- TẠI SAO PHẢI CÓ MIGRATION NÀY?
--   1. PostgreSQL KHÔNG tự động đánh index trên các Foreign Key (ngoại trừ Primary Key / Unique).
--      Khi không có index trên FK, các thao tác JOIN ngược hoặc ON DELETE CASCADE/SET NULL
--      sẽ buộc PostgreSQL phải Sequential Scan (Full Table Scan) toàn bộ bảng con.
--   2. Rất nhiều câu query sử dụng đồng thời bộ lọc (WHERE) và sắp xếp (ORDER BY), ví dụ:
--      dòng thời gian bài viết, tin nhắn phòng chat, thông báo, bạn bè... Nếu không có
--      Composite Index phù hợp, Database sẽ phải thực hiện In-Memory Sort rất tốn CPU và RAM.
--   3. Các quan hệ 2 chiều đối xứng (Friendships, Matches, Reactions...) hiện tại chỉ có
--      Unique Index trên (user_one, user_two) hoặc (user_id, target_id). Khi query theo
--      chiều ngược lại (user_two hoặc target_id), index cũ không có tác dụng.
--   4. Tận dụng Partial Index (chỉ mục từng phần) trên các cờ boolean / trạng thái
--      (is_read = FALSE, revoked = FALSE, is_active = TRUE) giúp giảm 80-95% dung lượng index,
--      tăng tốc độ ghi (INSERT/UPDATE) và tối đa hóa tốc độ đọc.
-- =====================================================================================


-- =====================================================================================
-- 0. DỌN DẸP / XÓA BỎ CÁC INDEX TRÙNG LẶP & DƯ THỪA TỪ CÁC MIGRATION TRƯỚC
-- =====================================================================================

-- 1. Trùng lặp giữa V4 và V6 trên bảng posts:
--    Trong V4 đã tạo: CREATE INDEX idx_posts_content_search ON posts USING gin(content gin_trgm_ops);
--    Nhưng trong V6 lại tạo lại: CREATE INDEX idx_posts_content_trgm ON posts USING gin(content gin_trgm_ops);
--    Hai GIN index này giống hệt nhau 100% trên cùng một cột TEXT 'content', gây tốn gấp đôi dung lượng
--    và làm chậm hiệu năng ghi dữ liệu của bảng posts. Xóa bỏ idx_posts_content_trgm (giữ lại idx_posts_content_search từ V4).
DROP INDEX IF EXISTS idx_posts_content_trgm;

-- 2. Trùng lặp trong V9 trên bảng story_media:
--    Cột story_id đã có ràng buộc 'story_id BIGINT NOT NULL UNIQUE' (PostgreSQL tự động tạo B-Tree index duy nhất).
--    Nhưng cuối file V9 lại tạo thêm: CREATE INDEX idx_story_media_story_id ON story_media(story_id);
--    Index này là bản sao trùng lặp hoàn toàn với index của UNIQUE constraint.
DROP INDEX IF EXISTS idx_story_media_story_id;

-- 3. Dư thừa trong V9 trên bảng story_views:
--    Bảng đã có Primary Key là (story_id, viewer_id) - B-Tree index trên cặp này đã hỗ trợ tìm kiếm theo tiền tố story_id.
--    Index idx_story_views_story_id ON story_views(story_id) là dư thừa (redundant prefix index).
DROP INDEX IF EXISTS idx_story_views_story_id;

-- 4. Dư thừa trong V9 trên bảng story_reactions:
--    Bảng đã có Unique Constraint: uk_story_user_reaction UNIQUE (story_id, user_id).
--    Index idx_story_reactions_story_id ON story_reactions(story_id) là dư thừa (redundant prefix index).
DROP INDEX IF EXISTS idx_story_reactions_story_id;


-- =====================================================================================
-- 1. AUTHENTICATION & TOKEN MODULE
-- =====================================================================================

-- [Bảng: refresh_tokens]
-- Query: RefreshTokenRepository.findByToken(String token)
-- Lý do: Mỗi request gia hạn Access Token đều tra cứu theo refresh token. Bảng hiện tại chưa
-- có ràng buộc UNIQUE hay INDEX nào trên cột 'token', dẫn đến Full Scan toàn bảng khi refresh token.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
    ON refresh_tokens(token);

-- Query: RefreshTokenRepository.findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc(...)
-- Lý do: Tra cứu token hợp lệ gần nhất của một user.
-- Sử dụng Partial Composite Index lọc trước 'revoked = FALSE', sắp xếp theo 'expired_at DESC'
-- để lấy ngay bản ghi đầu tiên (Index Scan) mà không cần quét các token đã bị thu hồi.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_valid
    ON refresh_tokens(user_id, expired_at DESC)
    WHERE revoked = FALSE;

-- [Bảng: email_verification_tokens]
-- Query: EmailVerificationTokenRepository.deleteByUser(User user) và ON DELETE CASCADE từ users(user_id).
-- Lý do: Khóa ngoại user_id chưa có index. Khi user xác thực xong hoặc bị xóa tài khoản,
-- DB cần index trên user_id để xóa token nhanh chóng thay vì quét toàn bộ bảng.
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id
    ON email_verification_tokens(user_id);

-- [Bảng: password_reset_tokens]
-- Query: PasswordResetTokenRepository.deleteByUser(User user) và ON DELETE CASCADE từ users(user_id).
-- Lý do: Tương tự token email, tối ưu thao tác dọn dẹp token quên mật khẩu theo user_id.
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);


-- =====================================================================================
-- 2. USER & PHÂN QUYỀN (RBAC)
-- =====================================================================================

-- [Bảng: users]
-- Query: UserRepository.findAllAdminUsers(@Param("status") Status status, Pageable pageable)
--   -> WHERE (:status IS NULL OR u.status = :status) ORDER BY u.createdAt DESC
-- Lý do: Trang quản trị (Admin User Management) lọc người dùng theo trạng thái và phân trang
-- từ người dùng mới nhất đến cũ nhất. Composite Index (status, created_at DESC) giúp tránh sort in-memory.
CREATE INDEX IF NOT EXISTS idx_users_status_created_at
    ON users(status, created_at DESC);

-- [Bảng: user_roles]
-- Query: UserRoleRepository.findUsersByRole(RoleName roleName)
-- Lý do: Bảng chỉ có Primary Key là (user_id, role_id). Khi cần truy vấn ngược: "Tìm tất cả users
-- có role ADMIN", DB bắt buộc phải quét toàn bộ bảng nếu không có index bắt đầu bằng role_id.
-- Đồng thời hỗ trợ FK ON DELETE CASCADE khi xóa một Role.
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id
    ON user_roles(role_id);


-- =====================================================================================
-- 3. QUAN HỆ BẠN BÈ & CHẶN (FRIENDSHIPS & BLOCKS)
-- =====================================================================================

-- [Bảng: friendships]
-- Bảng hiện tại chỉ có unique constraint: uk_friendship_pair UNIQUE(user_one_id, user_two_id).
-- Mọi câu query bạn bè (getMyFriends, getFriends, areFriends, findMutualFriends, findSuggestedUsers)
-- đều có điều kiện đối xứng: (user_one_id = :userId OR user_two_id = :userId) AND status = :status.
--
-- Lý do:
-- - uk_friendship_pair chỉ hỗ trợ tìm kiếm khi user_one_id = :userId.
-- - Khi user nằm ở vế user_two_id = :userId, DB không dùng được index cũ.
-- - Thêm 2 composite index cho cả 2 vế kèm cột status để Index Scan trực tiếp mà không cần lọc filter thêm.
CREATE INDEX IF NOT EXISTS idx_friendships_user_two_status
    ON friendships(user_two_id, status);

CREATE INDEX IF NOT EXISTS idx_friendships_user_one_status
    ON friendships(user_one_id, status);

-- Query: FriendshipRepository.getPendingFriendRequests(...)
--   -> WHERE (f.userOne.id = :userId OR f.userTwo.id = :userId) AND f.status = :status AND f.requester.id <> :userId
-- Lý do: Tối ưu lọc lời mời kết bạn gửi đến hoặc gửi đi dựa theo người gửi yêu cầu (requester_id) và status ('PENDING').
CREATE INDEX IF NOT EXISTS idx_friendships_requester_status
    ON friendships(requester_id, status);

-- [Bảng: blocks]
-- Query: BlockRepository.findBlockedUsers(@Param("userId") Long userId, Pageable pageable)
--   -> WHERE b.blocker.id = :userId ORDER BY b.createdAt DESC
-- Lý do: Khóa chính hiện tại là (blocker_id, blocked_id). Để phân trang danh sách người bị chặn
-- theo thời gian chặn mới nhất mà không phải Sort in-memory, cần index (blocker_id, created_at DESC).
CREATE INDEX IF NOT EXISTS idx_blocks_blocker_created
    ON blocks(blocker_id, created_at DESC);


-- =====================================================================================
-- 4. BÀI VIẾT (POSTS & MEDIA & HASHTAGS & SAVED POSTS)
-- =====================================================================================

-- [Bảng: posts]
-- Query: PostRepository.findAllPostMe, findAllVisiblePost, countByUserIdAndPostType
--   -> WHERE u.id = :userId AND p.postType = :postType ORDER BY p.createdAt DESC
-- Lý do: Index cũ idx_posts_user_created chỉ có (user_id, created_at DESC), thiếu post_type.
-- Khi tách bài đăng thường ('POST') và video ngắn ('REEL') trên trang cá nhân, DB phải lọc lại
-- từng dòng. Composite Index (user_id, post_type, created_at DESC) giải quyết triệt để vấn đề này.
CREATE INDEX IF NOT EXISTS idx_posts_user_type_created
    ON posts(user_id, post_type, created_at DESC);

-- Query: PostRepository.findFeed, findExplore
--   -> WHERE p.post_type = :postType AND p.visibility = :visibility ORDER BY p.created_at DESC
-- Lý do: Newfeed và trang Khám phá liên tục lọc bài viết theo loại bài đăng (POST/REEL),
-- mức độ hiển thị (PUBLIC/FRIEND) và lấy bài viết mới nhất. Composite index này giúp
-- Index Range Scan cực nhanh cho trang chủ mạng xã hội.
CREATE INDEX IF NOT EXISTS idx_posts_type_visibility_created
    ON posts(post_type, visibility, created_at DESC);

-- [Bảng: post_hashtags]
-- Query: PostRepository.searchByHashtag, PostHashtagRepository.getTrendingHashtags
--   -> JOIN ph.hashtag h WHERE LOWER(h.name) = ... (JOIN qua hashtag_id)
--   -> GROUP BY h.hashtagId ORDER BY COUNT(ph) DESC
-- Lý do: Unique constraint hiện tại là (post_id, hashtag_id), có post_id đứng trước.
-- Mọi truy vấn bắt đầu từ hashtag để tìm ra các post đều bị Sequential Scan bảng post_hashtags.
-- Index trên hashtag_id là bắt buộc cho tính năng tìm kiếm theo thẻ hashtag và bảng xếp hạng trending.
CREATE INDEX IF NOT EXISTS idx_post_hashtags_hashtag_id
    ON post_hashtags(hashtag_id);

-- [Bảng: saved_posts]
-- Query: PostRepository.findSavedPosts
--   -> WHERE sp.user.id = :userId ORDER BY sp.createdAt DESC
-- Lý do: Khóa chính là (user_id, post_id). Xem danh sách bài viết đã lưu cần sắp xếp theo
-- ngày lưu gần nhất (created_at DESC), tránh In-Memory Sort.
CREATE INDEX IF NOT EXISTS idx_saved_posts_user_created
    ON saved_posts(user_id, created_at DESC);

-- Query: FK ON DELETE CASCADE từ posts(post_id)
-- Lý do: Khi một bài viết bị xóa, nếu bảng saved_posts không có index trên post_id thì PostgreSQL
-- phải quét toàn bộ bảng saved_posts để xóa các lượt lưu của bài viết đó.
CREATE INDEX IF NOT EXISTS idx_saved_posts_post_id
    ON saved_posts(post_id);

-- [Bảng: post_media]
-- Query: PostMediaRepository.findByPublicId(String publicId)
-- Lý do: Khi upload lại hoặc xóa media trên Cloudinary, hệ thống tra cứu theo public_id.
-- Cột public_id chưa có index, index này giúp tra cứu O(log N).
CREATE INDEX IF NOT EXISTS idx_post_media_public_id
    ON post_media(public_id);


-- =====================================================================================
-- 5. BÌNH LUẬN & PHẢN HỒI (COMMENTS & REACTIONS)
-- =====================================================================================

-- [Bảng: comments]
-- Query: CommentRepository.findReplies(@Param("parentId") Long parentId, ...)
--   -> WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC
--   và countByParent_Id(Long parentId)
-- Lý do: Khi người dùng bấm "Xem phản hồi" của một bình luận, query lọc theo parent_id và
-- sắp xếp từ bình luận cũ nhất đến mới nhất. Cột parent_id trước đây chưa hề có index!
CREATE INDEX IF NOT EXISTS idx_comments_parent_created
    ON comments(parent_id, created_at ASC);

-- Query: CommentRepository.findRootComments(@Param("postId") Long postId, ...)
--   -> WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt DESC
-- Lý do: Khi mở bài viết, 95% lưu lượng đọc là lấy các bình luận gốc (cấp 1 - parent IS NULL).
-- Partial Index này chỉ đánh chỉ mục cho các root comment, kích thước index cực nhỏ,
-- giúp hiển thị bình luận bài viết với tốc độ tối đa.
CREATE INDEX IF NOT EXISTS idx_comments_post_root
    ON comments(post_id, created_at DESC)
    WHERE parent_id IS NULL;

-- Query: FK ON DELETE CASCADE từ users(user_id)
-- Lý do: Tối ưu thao tác dọn dẹp bình luận khi người dùng xóa tài khoản.
CREATE INDEX IF NOT EXISTS idx_comments_user_id
    ON comments(user_id);

-- [Bảng: reactions]
-- Query: ReactionRepository.countReactionTypesByPostId, findUsersReacted, countByPostId, countReactionsByPostIds
--   -> WHERE r.post.id = :postId GROUP BY r.type
--   -> WHERE r.post.id = :postId AND (:type IS NULL OR r.type = :type)
-- Lý do: Bảng reactions hiện CHỈ CÓ unique index (user_id, post_id). Mọi thao tác hiển thị
-- bài viết đều cần đếm số like, lấy danh sách người thả tim theo post_id. Vì post_id nằm ở vị trí
-- thứ 2 trong unique index, DB buộc phải Seq Scan toàn bộ bảng reactions (hàng triệu dòng!).
-- Index (post_id, type) cho phép Index-Only Scan đếm và nhóm reaction theo loại siêu nhanh.
CREATE INDEX IF NOT EXISTS idx_reactions_post_type
    ON reactions(post_id, type);

-- [Bảng: comment_reactions]
-- Query: CommentReactionRepository.countReactionsByCommentId, findUsersReacted, countByCommentId
--   -> WHERE r.comment.id = :commentId GROUP BY r.type
-- Lý do: Tương tự bảng reactions, comment_reactions chỉ có unique (user_id, comment_id).
-- Index (comment_id, type) giúp lấy và đếm các cảm xúc của bình luận cực nhanh và hỗ trợ cascade delete.
CREATE INDEX IF NOT EXISTS idx_comment_reactions_comment_type
    ON comment_reactions(comment_id, type);


-- =====================================================================================
-- 6. TIN NHẮN & PHÒNG CHAT (CHAT & CONVERSATIONS)
-- =====================================================================================

-- [Bảng: conversation_members]
-- Query: ConversationMemberRepository.findConversationsByUserId, countUnreadMessagesByConversationIds,
--        ConversationRepository.findConversationList, findPrivateConversation
--   -> WHERE cm.user.id = :userId
-- Lý do: Khóa chính của bảng là (conversation_id, user_id). Mọi câu query hiển thị danh sách hội thoại
-- của một user đều lọc theo user_id. Không có index này, mỗi lần user mở tab chat, DB phải quét toàn bộ
-- thành viên của TẤT CẢ các cuộc trò chuyện trong hệ thống.
CREATE INDEX IF NOT EXISTS idx_conversation_members_user_conv
    ON conversation_members(user_id, conversation_id);

-- [Bảng: conversations]
-- Query: ConversationRepository.findConversationList, findDatingConversationList
--   -> WHERE c.type = :type ORDER BY c.lastMessageAt DESC NULLS LAST
-- Lý do: Sắp xếp danh sách cuộc trò chuyện theo tin nhắn gần đây nhất.
CREATE INDEX IF NOT EXISTS idx_conversations_type_last_msg
    ON conversations(type, last_message_at DESC NULLS LAST);

-- [Bảng: messages]
-- BẢNG QUAN TRỌNG NHẤT VÀ TĂNG TRƯỞNG NHANH NHẤT TRONG HỆ THỐNG CHAT!
-- Hiện tại bảng messages CHƯA CÓ BẤT KỲ INDEX NÀO ngoại trừ Primary Key (message_id).
--
-- Query: MessageRepository.findMessages(@Param("conversationId") Long conversationId, Pageable pageable)
--   -> WHERE c.id = :conversationId ORDER BY m.createdAt DESC
-- Lý do: Load tin nhắn khi mở phòng chat. Index này là sống còn để tránh sập database chat.
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created
    ON messages(conversation_id, created_at DESC);

-- Query: MessageRepository.countUnreadMessages, findTopByConversationIdAndIdLessThan...
--   -> WHERE conversation_id = :id AND m.id > lastReadMessageId AND deleted = FALSE
--   -> WHERE conversation_id = :id AND id < :msgId AND deleted = FALSE ORDER BY id DESC
-- Lý do: Tối ưu đếm tin nhắn chưa đọc và cuộn xem tin nhắn cũ (infinite scroll theo ID).
-- Dùng Partial Index với 'is_deleted = FALSE' để loại bỏ các tin nhắn đã thu hồi.
CREATE INDEX IF NOT EXISTS idx_messages_conversation_active_id
    ON messages(conversation_id, message_id ASC)
    WHERE is_deleted = FALSE;

-- Query: FK ON DELETE CASCADE từ users(sender_id)
-- Lý do: Tối ưu khóa ngoại người gửi và tra cứu lịch sử gửi tin nhắn.
CREATE INDEX IF NOT EXISTS idx_messages_sender_id
    ON messages(sender_id);

-- Query: Tra cứu reply_to_message_id (thread tin nhắn trả lời)
-- Lý do: Partial index loại bỏ các tin nhắn không phải reply (NULL), tối ưu hiệu năng và tiết kiệm ổ đĩa.
CREATE INDEX IF NOT EXISTS idx_messages_reply_to
    ON messages(reply_to_message_id)
    WHERE reply_to_message_id IS NOT NULL;

-- [Bảng: message_media]
-- Query: MessageMediaRepository.findByMessageIds(List<Long> messageIds)
-- Lý do: Tải danh sách hình ảnh / video đính kèm của các tin nhắn trong đoạn chat.
CREATE INDEX IF NOT EXISTS idx_message_media_message_id
    ON message_media(message_id);

-- [Bảng: message_reactions]
-- Query: MessageReactionRepository.findByMessageIds, findAllByMessageId, findUsersReacted
--   -> WHERE mr.message.id = :messageId ORDER BY mr.createdAt DESC
-- Lý do: Bảng hiện tại chỉ có unique index (user_id, message_id). Index bắt đầu bằng message_id
-- giúp load các reaction thả trên tin nhắn trong phòng chat.
CREATE INDEX IF NOT EXISTS idx_message_reactions_message_created
    ON message_reactions(message_id, created_at DESC);


-- =====================================================================================
-- 7. THÔNG BÁO (NOTIFICATIONS)
-- =====================================================================================

-- [Bảng: notifications]
-- Hiện tại bảng notifications CHƯA CÓ BẤT KỲ INDEX NÀO ngoài Primary Key (notification_id)!
--
-- Query: NotificationRepository.findMyNotifications(@Param("receiverId") Long receiverId, Pageable pageable)
--   -> WHERE n.receiver.id = :receiverId ORDER BY n.createdAt DESC
-- Lý do: Lấy danh sách thông báo của người dùng với phân trang từ mới nhất đến cũ nhất.
CREATE INDEX IF NOT EXISTS idx_notifications_receiver_created
    ON notifications(receiver_id, created_at DESC);

-- Query: NotificationRepository.findMyUnreadNotifications, countByReceiver_IdAndIsReadFalse, markAllAsRead
--   -> WHERE n.receiver.id = :receiverId AND n.isRead = FALSE ORDER BY n.createdAt DESC
-- Lý do: Tối ưu hóa tuyệt đối cho tính năng đếm thông báo đỏ (badge unread count) và tab thông báo chưa đọc.
-- Vì đa số thông báo sau khi đọc sẽ chuyển thành is_read = TRUE, Partial Index này chỉ chứa số ít
-- thông báo chưa đọc nên kích thước cực nhỏ và tốc độ phản hồi tính bằng micro-giây.
CREATE INDEX IF NOT EXISTS idx_notifications_receiver_unread
    ON notifications(receiver_id, created_at DESC)
    WHERE is_read = FALSE;

-- Query: NotificationRepository.existsByReceiver_IdAndSender_Id..., deleteByReceiver_IdAndSender_Id...
--   -> WHERE receiver_id = :r AND sender_id = :s AND entity_type = :et AND entity_id = :ei AND type = :t
-- Lý do: Ngăn trùng lặp thông báo (Deduplication) khi 1 người like/unlike liên tục và xóa thông báo khi hủy tương tác.
CREATE INDEX IF NOT EXISTS idx_notifications_dedup
    ON notifications(receiver_id, sender_id, entity_type, entity_id, type);

-- Query: FK ON DELETE CASCADE từ users(sender_id)
-- Lý do: Tránh Seq Scan khi người gửi bị xóa tài khoản.
CREATE INDEX IF NOT EXISTS idx_notifications_sender_id
    ON notifications(sender_id);


-- =====================================================================================
-- 8. VIDEO NGẮN (REELS)
-- =====================================================================================

-- [Bảng: reel_views]
-- Query: FK user_id ON DELETE SET NULL
-- Lý do: Cột user_id trong reel_views cho phép NULL (user ẩn danh hoặc tài khoản bị xóa).
-- Bảng hiện có unique index (reel_id, user_id). Index riêng trên user_id giúp tối ưu thao tác
-- SET NULL khi user bị xóa tài khoản và tra cứu lịch sử xem reels của một user.
CREATE INDEX IF NOT EXISTS idx_reel_views_user_id
    ON reel_views(user_id)
    WHERE user_id IS NOT NULL;


-- =====================================================================================
-- 9. HẸN HÒ (DATING MODULE)
-- =====================================================================================

-- [Bảng: dating_profiles]
-- Query: DatingProfileRepository.findDiscoveryCandidates
--   -> WHERE target.is_active = TRUE AND target.gender = :genderPreference ...
-- Lý do: Thuật toán Discovery (quẹt thẻ tìm bạn hẹn hò) liên tục quét các hồ sơ đang kích hoạt
-- theo giới tính mong muốn và độ tuổi (birthday). Partial Index này lọc sẵn các hồ sơ active.
CREATE INDEX IF NOT EXISTS idx_dating_profiles_discovery
    ON dating_profiles(gender, birthday)
    WHERE is_active = TRUE;

-- [Bảng: dating_matches]
-- Query: DatingMatchRepository.findActiveMatchesByUserId(@Param("userId") Long userId)
--   -> WHERE (m.userOne.id = :userId OR m.userTwo.id = :userId) AND m.status = 'ACTIVE' ORDER BY m.matchedAt DESC
-- Lý do: Unique index hiện có chỉ là uk_match_pair(user_one_id, user_two_id).
-- Khi user là user_two_id thì không tận dụng được index. Tạo 2 Partial Index lọc trước status = 'ACTIVE'
-- và sắp xếp matched_at DESC cho cả 2 nhánh user_one và user_two.
CREATE INDEX IF NOT EXISTS idx_dating_matches_user_one_active
    ON dating_matches(user_one_id, matched_at DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_dating_matches_user_two_active
    ON dating_matches(user_two_id, matched_at DESC)
    WHERE status = 'ACTIVE';

-- [Bảng: dating_swipes]
-- Query: DatingSwipeRepository.findAllBySwiperIdOrderByCreatedAtDesc, findAllBySwiperIdAndActionOrderByCreatedAtDesc
--   -> WHERE ds.swiper.id = :swiperId AND ds.action = :action ORDER BY ds.createdAt DESC
-- Lý do: Tối ưu xem lịch sử quẹt thẻ (những ai mình đã Like/Dislike/Super Like) theo thứ tự thời gian.
CREATE INDEX IF NOT EXISTS idx_dating_swipes_swiper_action_created
    ON dating_swipes(swiper_id, action, created_at DESC);

-- Query: Subquery Discovery (loại trừ người mình đã quẹt) & FK ON DELETE CASCADE từ target_id
--   -> NOT EXISTS (SELECT 1 FROM dating_swipes swipe WHERE swipe.swiper_id = :me AND swipe.target_id = target.user_id)
-- Lý do: Unique index là (swiper_id, target_id). Thêm index trên target_id để hỗ trợ FK cascade
-- và câu query kiểm tra ai đã quẹt mình.
CREATE INDEX IF NOT EXISTS idx_dating_swipes_target_id
    ON dating_swipes(target_id);

-- [Bảng: dating_profile_photos]
-- Query: DatingProfilePhotoRepository.findTopByDatingProfileIdOrderByDisplayOrderAsc/Desc,
--        countByDatingProfileId, JOIN trong Discovery query
-- Lý do: Migration V7 chỉ có unique partial index cho is_primary = TRUE. Thiếu index tổng thể
-- trên dating_profile_id. Index (dating_profile_id, display_order ASC) giúp lấy danh sách ảnh
-- của một profile theo đúng thứ tự hiển thị mà không cần in-memory sort.
CREATE INDEX IF NOT EXISTS idx_dating_profile_photos_profile_order
    ON dating_profile_photos(dating_profile_id, display_order ASC);

-- [Bảng: dating_profile_interests]
-- Query: Subquery / JOIN tìm bạn chung sở thích trong Dating Discovery:
--   -> ON common_interest.dating_profile_id = target.dating_profile_id AND common_interest.interest_id = my_interest.interest_id
-- Lý do: Khóa chính là (dating_profile_id, interest_id). Tra cứu từ interest_id để tìm các profile
-- có chung sở thích bị quét toàn bảng nếu không có index trên interest_id.
CREATE INDEX IF NOT EXISTS idx_dating_profile_interests_interest_id
    ON dating_profile_interests(interest_id);

-- [Bảng: dating_reports]
-- Query: DatingReportRepository.getByStatus, Subquery Discovery loại trừ tài khoản bị báo cáo
-- Lý do: Bảng chưa hề có index ngoài Primary Key (report_id).
-- Tạo index hỗ trợ lọc báo cáo vi phạm theo cặp reporter-reported và theo trạng thái duyệt admin.
CREATE INDEX IF NOT EXISTS idx_dating_reports_reporter_reported
    ON dating_reports(reporter_id, reported_user_id);

CREATE INDEX IF NOT EXISTS idx_dating_reports_reported_user
    ON dating_reports(reported_user_id);

CREATE INDEX IF NOT EXISTS idx_dating_reports_status_created
    ON dating_reports(status, created_at DESC);


-- =====================================================================================
-- 10. BÁO CÁO VI PHẠM BÀI VIẾT (REPORTS)
-- =====================================================================================

-- [Bảng: reports]
-- Query: ReportRepository.getByStatus(@Param("status") ReportStatus status, Pageable pageable)
--   -> WHERE r.status = :status ORDER BY r.createdAt DESC
--   và ReportRepository.getAll(Pageable pageable)
-- Lý do: Index cũ idx_reports_post_status chỉ phục vụ tìm report theo (post_id, status).
-- Admin Dashboard cần duyệt các báo cáo vi phạm mới nhất theo trạng thái (PENDING/APPROVED/REJECTED).
CREATE INDEX IF NOT EXISTS idx_reports_status_created
    ON reports(status, created_at DESC);
