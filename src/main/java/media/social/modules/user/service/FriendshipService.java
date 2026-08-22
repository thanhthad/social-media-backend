package media.social.modules.user.service;

import media.social.modules.user.dto.response.friend.FriendSuggestionResponse;
import media.social.modules.user.dto.response.friend.MutualFriendResponse;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FriendshipService {

    void sendFriendRequest(Long targetUserId);

    void acceptFriendRequest(Long requesterId);

    void rejectFriendRequest(Long requesterId);

    void cancelFriendRequest(Long targetUserId);
    /**
     * Lấy danh sách bạn bè của một user.
     * Nếu xem user khác, danh sách sẽ được kiểm tra theo quyền riêng tư.
     */
    Page<FriendshipUserResponse> getFriends(
            Long userId,
            Pageable pageable
    );

    /**
     * Lấy danh sách lời mời kết bạn đang chờ mà user hiện tại đã nhận.
     */
    Page<FriendshipUserResponse> getPendingFriendRequests(
            Pageable pageable
    );

    /**
     * Lấy danh sách người dùng được gợi ý kết bạn
     * dựa trên số lượng bạn chung với user hiện tại.
     */
    List<FriendSuggestionResponse> getSuggestedUsers();

    /**
     * Lấy danh sách những người là bạn chung giữa
     * user hiện tại và user được chỉ định , bao gồm cả chưa kết bạn và đã là bạn bè
     */
    List<MutualFriendResponse> getMutualFriends(Long targetUserId);
}

