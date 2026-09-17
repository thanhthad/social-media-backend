package media.social.modules.post.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.CommentProjection;
import media.social.modules.post.dto.projection.RootCommentProjection;
import media.social.modules.post.dto.projection.UserReactionProjection;
import media.social.modules.post.entity.*;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho {@link CommentRepository}, {@link ReactionRepository},
 * và {@link CommentReactionRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1: bảng comments, reactions, comment_reactions, các ràng buộc unique_user_post_reaction, uk_user_comment_reaction)</li>
 *     <li>Root comments & Replies (cấu trúc comment lồng nhau, COUNT DISTINCT replies/reactions, myReaction)</li>
 *     <li>GROUP BY + COUNT (thống kê phân loại ReactionType theo postId và commentId)</li>
 *     <li>Pagination & ORDER BY (findRootComments DESC, findReplies ASC)</li>
 *     <li>@Modifying updateContent của bình luận</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Comment & Reaction – Integration Tests với PostgreSQL Container")
class CommentAndReactionRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_comment_reaction_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private CommentReactionRepository commentReactionRepository;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;
    private Post post;
    private Comment rootComment;
    private Comment replyComment;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");

        createProfile(alice, "Alice Nguyen", "https://cdn.example.com/alice.jpg");
        createProfile(bob, "Bob Tran", "https://cdn.example.com/bob.jpg");
        createProfile(charlie, "Charlie Le", "https://cdn.example.com/charlie.jpg");

        post = createPost(alice, "Post with comments and reactions");

        // Comments: Root comment by bob
        rootComment = createComment(post, bob, null, "Great post!");

        // Reply to rootComment by charlie
        replyComment = createComment(post, charlie, rootComment, "Totally agree!");

        // Reactions on post
        createPostReaction(post, bob, ReactionType.LIKE);
        createPostReaction(post, charlie, ReactionType.LOVE);

        // Reactions on comment
        createCommentReaction(rootComment, alice, ReactionType.LIKE);
        createCommentReaction(rootComment, charlie, ReactionType.LIKE);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1 Schema & Unique Constraints)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: unique_user_post_reaction ngăn duplicate reaction trên cùng 1 bài viết")
        void flyway_uniqueUserPostReactionConstraint() {
            // bob đã react post trong setUp, thử react lần 2
            Reaction duplicate = Reaction.builder()
                    .post(post)
                    .user(bob)
                    .type(ReactionType.HAHA)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("unique_user_post_reaction");
        }

        @Test
        @DisplayName("Flyway V1: uk_user_comment_reaction ngăn duplicate reaction trên cùng 1 bình luận")
        void flyway_uniqueUserCommentReactionConstraint() {
            // alice đã react rootComment trong setUp, thử react lần 2
            CommentReaction duplicate = CommentReaction.builder()
                    .comment(rootComment)
                    .user(alice)
                    .type(ReactionType.LOVE)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_user_comment_reaction");
        }
    }

    // =========================================================================
    // 2. COMMENT REPOSITORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. CommentRepository Queries (RootComments, Replies, Modifying)")
    class CommentQueriesTests {

        @Test
        @DisplayName("findRootComments: Lấy bình luận gốc kèm tổng số replies, reactions và trạng thái myReaction")
        void findRootComments_returnsAggregatedProjection() {
            // viewerId = alice (alice đã like rootComment)
            Page<RootCommentProjection> page = commentRepository.findRootComments(
                    post.getId(), alice.getId(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
            RootCommentProjection root = page.getContent().get(0);
            assertThat(root.getContent()).isEqualTo("Great post!");
            assertThat(root.getUsername()).isEqualTo("bob");
            assertThat(root.getTotalReplies()).isEqualTo(1L); // replyComment của charlie
            assertThat(root.getTotalReactions()).isEqualTo(2L); // alice & charlie
            assertThat(root.getMyReaction()).isEqualTo(ReactionType.LIKE);
        }

        @Test
        @DisplayName("findReplies: Lấy danh sách phản hồi theo parentId sắp xếp tăng dần theo thời gian (createdAt ASC)")
        void findReplies_orderedByCreatedAtAsc() {
            // Tạo thêm reply thứ 2
            createComment(post, alice, rootComment, "Reply 2 from Alice");
            em.flush();
            em.clear();

            Page<CommentProjection> replies = commentRepository.findReplies(
                    rootComment.getId(), bob.getId(), PageRequest.of(0, 10));

            assertThat(replies.getContent()).hasSize(2);
            assertThat(replies.getContent().get(0).getContent()).isEqualTo("Totally agree!");
            assertThat(replies.getContent().get(1).getContent()).isEqualTo("Reply 2 from Alice");
        }

        @Test
        @DisplayName("updateContent: @Modifying cập nhật nội dung bình luận đúng tác giả")
        void updateContent_modifiesContent() {
            int updated = commentRepository.updateContent(rootComment.getId(), bob.getId(), "Updated comment content");
            em.flush();
            em.clear();

            assertThat(updated).isEqualTo(1);
            Comment modified = commentRepository.findById(rootComment.getId()).orElseThrow();
            assertThat(modified.getContent()).isEqualTo("Updated comment content");
        }

        @Test
        @DisplayName("countByParent_Id & countByPostId: Đếm số lượng bình luận chuẩn xác")
        void countComments_accurate() {
            assertThat(commentRepository.countByParent_Id(rootComment.getId())).isEqualTo(1L);
            assertThat(commentRepository.countByPostId(post.getId())).isEqualTo(2L);
        }
    }

    // =========================================================================
    // 3. REACTION REPOSITORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. ReactionRepository Queries (GROUP BY, findUsersReacted)")
    class ReactionQueriesTests {

        @Test
        @DisplayName("countReactionTypesByPostId: GROUP BY r.type đếm số lượng từng loại cảm xúc")
        void countReactionTypesByPostId_groupByType() {
            List<Object[]> reactionCounts = reactionRepository.countReactionTypesByPostId(post.getId());

            // Có 1 LIKE (bob) và 1 LOVE (charlie)
            assertThat(reactionCounts).hasSize(2);
            for (Object[] row : reactionCounts) {
                ReactionType type = (ReactionType) row[0];
                Long count = (Long) row[1];
                assertThat(count).isEqualTo(1L);
                assertThat(type).isIn(ReactionType.LIKE, ReactionType.LOVE);
            }
        }

        @Test
        @DisplayName("findUsersReacted: Lấy danh sách user react kèm lọc type (nullable filter)")
        void findUsersReacted_withTypeFilter() {
            // Lọc tất cả (type = null)
            Page<UserReactionProjection> allReacted = reactionRepository.findUsersReacted(
                    post.getId(), null, PageRequest.of(0, 10));
            assertThat(allReacted.getContent()).hasSize(2);

            // Lọc riêng type = LIKE
            Page<UserReactionProjection> likesOnly = reactionRepository.findUsersReacted(
                    post.getId(), ReactionType.LIKE, PageRequest.of(0, 10));
            assertThat(likesOnly.getContent()).hasSize(1);
            assertThat(likesOnly.getContent().get(0).getUserName()).isEqualTo("bob");
        }

        @Test
        @DisplayName("findMyReactions: Tìm danh sách cảm xúc của user trên tập hợp các bài viết")
        void findMyReactions_byPostIds() {
            List<Reaction> myReactions = reactionRepository.findMyReactions(bob.getId(), List.of(post.getId()));

            assertThat(myReactions).hasSize(1);
            assertThat(myReactions.get(0).getType()).isEqualTo(ReactionType.LIKE);
        }
    }

    // =========================================================================
    // 4. COMMENT REACTION REPOSITORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. CommentReactionRepository Queries")
    class CommentReactionQueriesTests {

        @Test
        @DisplayName("countReactionsByCommentId: GROUP BY r.type trên comment")
        void countReactionsByCommentId_groupBy() {
            List<Object[]> counts = commentReactionRepository.countReactionsByCommentId(rootComment.getId());

            // Có 2 LIKE từ alice và charlie
            assertThat(counts).hasSize(1);
            assertThat(counts.get(0)[0]).isEqualTo(ReactionType.LIKE);
            assertThat(counts.get(0)[1]).isEqualTo(2L);
        }

        @Test
        @DisplayName("findReactionType: Lấy đúng ReactionType của user trên bình luận")
        void findReactionType_accurate() {
            ReactionType type = commentReactionRepository.findReactionType(alice.getId(), rootComment.getId());
            assertThat(type).isEqualTo(ReactionType.LIKE);

            ReactionType noType = commentReactionRepository.findReactionType(bob.getId(), rootComment.getId());
            assertThat(noType).isNull();
        }

        @Test
        @DisplayName("deleteByUserIdAndCommentId: Gỡ bỏ cảm xúc trên comment")
        void deleteByUserIdAndCommentId_removesReaction() {
            commentReactionRepository.deleteByUserIdAndCommentId(alice.getId(), rootComment.getId());
            em.flush();
            em.clear();

            assertThat(commentReactionRepository.existsByUserIdAndCommentId(alice.getId(), rootComment.getId()))
                    .isFalse();
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUser(String username, String email) {
        return em.persist(User.builder()
                .username(username)
                .email(email)
                .passwordHash("$2a$10$dummyHash")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .status(Status.ACTIVE)
                .build());
    }

    private void createProfile(User user, String fullName, String avatarUrl) {
        em.persist(Profile.builder()
                .user(user)
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .dateOfBirth(LocalDate.of(1997, 1, 1))
                .visibility(Visibility.PUBLIC)
                .build());
    }

    private Post createPost(User user, String content) {
        return em.persist(Post.builder()
                .user(user)
                .content(content)
                .postType(PostType.POST)
                .visibility(Visibility.PUBLIC)
                .commentCount(0L)
                .reactionCount(0L)
                .build());
    }

    private Comment createComment(Post post, User user, Comment parent, String content) {
        return em.persist(Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(content)
                .build());
    }

    private void createPostReaction(Post post, User user, ReactionType type) {
        em.persist(Reaction.builder()
                .post(post)
                .user(user)
                .type(type)
                .build());
    }

    private void createCommentReaction(Comment comment, User user, ReactionType type) {
        em.persist(CommentReaction.builder()
                .comment(comment)
                .user(user)
                .type(type)
                .build());
    }
}
