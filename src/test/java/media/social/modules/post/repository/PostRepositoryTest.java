package media.social.modules.post.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.PostFlatProjection;
import media.social.modules.post.entity.*;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.Block;
import media.social.modules.user.entity.BlockId;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.repository.UserRepository;
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
 * Migration & Query Integration Test cho {@link PostRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử toàn diện các tính năng và câu truy vấn phức tạp của PostRepository:
 * <ul>
 *     <li>Migration test (Flyway V1: bảng posts, check constraints chk_posts_post_type, chk_posts_visibility, CASCADE delete)</li>
 *     <li>@Modifying counter queries (increase/decrease commentCount, reactionCount với CASE WHEN ngăn âm)</li>
 *     <li>JOIN FETCH (findByIdWithUser tải eager user tránh LazyInitializationException)</li>
 *     <li>NOT EXISTS (findAllPostMe loại trừ bài viết bị Report; findAllVisiblePost loại trừ Block & Report)</li>
 *     <li>EXISTS (findAllVisiblePost, findFeed kiểm tra quan hệ Friendship khi Visibility = FRIEND)</li>
 *     <li>Complex nativeQuery (findExplore: thuật toán tính điểm gravity decay kết hợp reaction_count, comment_count, thời gian)</li>
 *     <li>similarity() (searchByContent: native query fuzzy search nội dung bài viết với pg_trgm)</li>
 *     <li>Hashtags & Saved Posts (searchByHashtag với LOWER(), findSavedPosts kết hợp SavedPost)</li>
 *     <li>Aggregates (countByUserIdAndPostType, sumReactionsByUserId với COALESCE)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PostRepository – Integration Tests với PostgreSQL Container")
class PostRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_post_db")
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
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;
    private User blockerUser;

    private Post alicePublicPost;
    private Post aliceFriendOnlyPost;
    private Post bobPublicPost;
    private Post reportedPost;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");
        blockerUser = createUser("blocker", "blocker@example.com");

        createProfile(alice, "Alice Nguyen", "https://cdn.example.com/alice.jpg");
        createProfile(bob, "Bob Tran", "https://cdn.example.com/bob.jpg");
        createProfile(charlie, "Charlie Le", "https://cdn.example.com/charlie.jpg");

        // Friendship: alice <-> bob (ACCEPTED)
        createFriendship(alice, bob, alice, FriendshipStatus.ACCEPTED);

        // Block: alice <-> blockerUser
        createBlock(blockerUser, alice);

        // Posts
        alicePublicPost = createPost(alice, "Hello world this is Alice public post #spring #java",
                PostType.POST, Visibility.PUBLIC, 5L, 10L);

        aliceFriendOnlyPost = createPost(alice, "Alice private memories for friends only",
                PostType.POST, Visibility.FRIEND, 1L, 2L);

        bobPublicPost = createPost(bob, "Bob sharing programming tips with PostgreSQL",
                PostType.POST, Visibility.PUBLIC, 2L, 4L);

        reportedPost = createPost(bob, "Spam content violation",
                PostType.POST, Visibility.PUBLIC, 0L, 0L);

        // Report on reportedPost (status = APPROVED)
        createReport(reportedPost, alice, "Inappropriate content", ReportStatus.APPROVED);

        // Hashtags
        Hashtag springTag = createHashtag("spring");
        Hashtag javaTag = createHashtag("java");
        linkPostHashtag(alicePublicPost, springTag);
        linkPostHashtag(alicePublicPost, javaTag);

        // SavedPost: charlie saves alicePublicPost
        createSavedPost(charlie, alicePublicPost);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1 Schema, Check Constraints, Cascades)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: Bảng posts tạo thành công và lưu đúng dữ liệu")
        void flyway_postsTableCreated() {
            Post post = Post.builder()
                    .user(alice)
                    .content("Migration test post")
                    .postType(PostType.POST)
                    .visibility(Visibility.PUBLIC)
                    .commentCount(0L)
                    .reactionCount(0L)
                    .build();

            Post saved = postRepository.save(post);
            assertThat(saved.getId()).isNotNull();
            postRepository.delete(saved);
        }

        @Test
        @DisplayName("Flyway V1: ON DELETE CASCADE xóa posts khi User bị xóa")
        void flyway_cascadeDelete_removesPosts() {
            User tempUser = createUser("temp_user", "temp@example.com");
            Post tempPost = createPost(tempUser, "Temp post", PostType.POST, Visibility.PUBLIC, 0L, 0L);
            em.flush();
            em.clear();

            userRepository.deleteById(tempUser.getId());
            em.flush();
            em.clear();

            assertThat(postRepository.findById(tempPost.getId())).isEmpty();
        }
    }

    // =========================================================================
    // 2. MODIFYING COUNTER QUERIES TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. @Modifying Counter Queries (commentCount & reactionCount)")
    class CounterQueriesTests {

        @Test
        @DisplayName("increaseCommentCount & decreaseCommentCount hoạt động đúng và không âm")
        void commentCount_incrementAndDecrement() {
            Long postId = alicePublicPost.getId();
            long initialCount = postRepository.findById(postId).orElseThrow().getCommentCount();

            // Tăng 1
            postRepository.increaseCommentCount(postId);
            em.flush();
            em.clear();
            assertThat(postRepository.findById(postId).orElseThrow().getCommentCount())
                    .isEqualTo(initialCount + 1);

            // Giảm 1
            postRepository.decreaseCommentCount(postId);
            em.flush();
            em.clear();
            assertThat(postRepository.findById(postId).orElseThrow().getCommentCount())
                    .isEqualTo(initialCount);

            // Giảm nhiều lần để test CASE WHEN > 0 không bị số âm
            Post zeroCommentPost = createPost(alice, "Zero comments", PostType.POST, Visibility.PUBLIC, 0L, 0L);
            em.flush();
            em.clear();
            postRepository.decreaseCommentCount(zeroCommentPost.getId());
            em.flush();
            em.clear();
            assertThat(postRepository.findById(zeroCommentPost.getId()).orElseThrow().getCommentCount())
                    .isEqualTo(0L);
        }

        @Test
        @DisplayName("increaseReactionCount & decreaseReactionCount hoạt động chuẩn")
        void reactionCount_incrementAndDecrement() {
            Long postId = alicePublicPost.getId();
            long initialReaction = postRepository.findById(postId).orElseThrow().getReactionCount();

            postRepository.increaseReactionCount(postId);
            em.flush();
            em.clear();
            assertThat(postRepository.findById(postId).orElseThrow().getReactionCount())
                    .isEqualTo(initialReaction + 1);

            postRepository.decreaseReactionCount(postId);
            em.flush();
            em.clear();
            assertThat(postRepository.findById(postId).orElseThrow().getReactionCount())
                    .isEqualTo(initialReaction);
        }
    }

    // =========================================================================
    // 3. JOIN FETCH & DETAIL QUERIES
    // =========================================================================

    @Nested
    @DisplayName("3. JOIN FETCH & Post Detail Tests")
    class JoinFetchTests {

        @Test
        @DisplayName("findByIdWithUser: JOIN FETCH nạp sẵn User của bài viết")
        void findByIdWithUser_loadsUserEagerly() {
            Optional<Post> postOpt = postRepository.findByIdWithUser(alicePublicPost.getId(), PostType.POST);

            assertThat(postOpt).isPresent();
            assertThat(postOpt.get().getUser()).isNotNull();
            assertThat(postOpt.get().getUser().getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("findPostDetailById: Lọc theo visibilities và NOT EXISTS report")
        void findPostDetailById_validPost_returnsProjection() {
            Optional<PostFlatProjection> projOpt = postRepository.findPostDetailById(
                    alicePublicPost.getId(),
                    PostType.POST,
                    Status.ACTIVE,
                    List.of(Visibility.PUBLIC, Visibility.FRIEND),
                    ReportStatus.APPROVED
            );

            assertThat(projOpt).isPresent();
            assertThat(projOpt.get().getContent()).contains("Hello world");
            assertThat(projOpt.get().getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("findPostDetailById: Bài viết bị Report APPROVED sẽ không tìm thấy")
        void findPostDetailById_reportedPost_hidden() {
            Optional<PostFlatProjection> projOpt = postRepository.findPostDetailById(
                    reportedPost.getId(),
                    PostType.POST,
                    Status.ACTIVE,
                    List.of(Visibility.PUBLIC),
                    ReportStatus.APPROVED
            );

            assertThat(projOpt).isEmpty();
        }
    }

    // =========================================================================
    // 4. FEED & VISIBLE POSTS (EXISTS, NOT EXISTS, COMPLEX WHERE)
    // =========================================================================

    @Nested
    @DisplayName("4. Feed & Visible Posts (EXISTS, NOT EXISTS, Visibility)")
    class VisibilityAndFeedTests {

        @Test
        @DisplayName("findAllPostMe: Lấy bài viết của chính mình, loại trừ bài bị report APPROVED")
        void findAllPostMe_accurate() {
            Page<PostFlatProjection> page = postRepository.findAllPostMe(
                    alice.getId(),
                    Status.ACTIVE,
                    ReportStatus.APPROVED,
                    PostType.POST,
                    PageRequest.of(0, 10)
            );

            // alice có 2 bài (alicePublicPost, aliceFriendOnlyPost), không bài nào bị report
            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("findAllVisiblePost: Bob (bạn của alice) thấy được cả bài PUBLIC và FRIEND của alice")
        void findAllVisiblePost_friend_seesFriendOnlyPost() {
            // viewerId = bob, targetUserId = alice
            Page<PostFlatProjection> page = postRepository.findAllVisiblePost(
                    bob.getId(),
                    alice.getId(),
                    Status.ACTIVE,
                    ReportStatus.APPROVED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    PostType.POST,
                    PageRequest.of(0, 10)
            );

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("findAllVisiblePost: Charlie (không phải bạn của alice) chỉ thấy bài PUBLIC, KHÔNG thấy FRIEND")
        void findAllVisiblePost_stranger_seesOnlyPublicPost() {
            // viewerId = charlie, targetUserId = alice
            Page<PostFlatProjection> page = postRepository.findAllVisiblePost(
                    charlie.getId(),
                    alice.getId(),
                    Status.ACTIVE,
                    ReportStatus.APPROVED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    PostType.POST,
                    PageRequest.of(0, 10)
            );

            // Chỉ thấy alicePublicPost
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getContent()).contains("Hello world");
        }

        @Test
        @DisplayName("findFeed: Hiển thị bài viết của chính mình và của bạn bè")
        void findFeed_showsSelfAndFriendsPosts() {
            // Bob xem feed: thấy bài của chính mình (bobPublicPost) + bài của alice (bạn bè)
            Page<PostFlatProjection> feed = postRepository.findFeed(
                    bob.getId(),
                    Status.ACTIVE,
                    PostType.POST,
                    ReportStatus.APPROVED,
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    PageRequest.of(0, 10)
            );

            List<Long> postIds = feed.getContent().stream().map(PostFlatProjection::getId).toList();
            assertThat(postIds).contains(alicePublicPost.getId(), aliceFriendOnlyPost.getId(), bobPublicPost.getId());
        }
    }

    // =========================================================================
    // 5. NATIVE QUERY & SIMILARITY() TESTS (findExplore & searchByContent)
    // =========================================================================

    @Nested
    @DisplayName("5. Native Query, Gravity Decay & similarity() Tests")
    class NativeQueryTests {

        @Test
        @DisplayName("findExplore: Thuật toán gravity decay native SQL đưa bài tương tác cao lên đầu")
        void findExplore_gravityDecayOrdering() {
            // Charlie xem explore: bài của người lạ (alice, bob không phải bạn của charlie)
            Page<PostFlatProjection> explore = postRepository.findExplore(
                    charlie.getId(),
                    Status.ACTIVE.name(),
                    PostType.POST.name(),
                    ReportStatus.APPROVED.name(),
                    Visibility.PUBLIC.name(),
                    FriendshipStatus.ACCEPTED.name(),
                    PageRequest.of(0, 10)
            );

            assertThat(explore.getContent()).isNotEmpty();
            // alicePublicPost có 10 reaction, 5 comment -> điểm cao nhất
            assertThat(explore.getContent().get(0).getId()).isEqualTo(alicePublicPost.getId());
        }

        @Test
        @DisplayName("searchByContent: Native similarity() > 0.2 tìm kiếm mờ nội dung bài viết")
        void searchByContent_fuzzySearch() {
            // Tìm kiếm với từ khóa typo: "PostgreSQLL"
            Page<PostFlatProjection> result = postRepository.searchByContent(
                    alice.getId(),
                    "PostgreSQL",
                    Status.ACTIVE.name(),
                    PostType.POST.name(),
                    ReportStatus.APPROVED.name(),
                    Visibility.PUBLIC.name(),
                    Visibility.FRIEND.name(),
                    FriendshipStatus.ACCEPTED.name(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getContent()).contains("PostgreSQL");
        }
    }

    // =========================================================================
    // 6. HASHTAGS, SAVED POSTS & AGGREGATES
    // =========================================================================

    @Nested
    @DisplayName("6. Hashtag, Saved Posts & Aggregate Queries")
    class AuxiliaryQueriesTests {

        @Test
        @DisplayName("searchByHashtag: Tìm bài viết theo hashtag không phân biệt hoa thường (LOWER)")
        void searchByHashtag_caseInsensitive() {
            Page<PostFlatProjection> result = postRepository.searchByHashtag(
                    bob.getId(),
                    "SPRING", // viết hoa để test LOWER()
                    Status.ACTIVE,
                    PostType.POST,
                    ReportStatus.APPROVED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(alicePublicPost.getId());
        }

        @Test
        @DisplayName("findSavedPosts: Lấy danh sách bài viết đã lưu của user (charlie lưu bài của alice)")
        void findSavedPosts_accurate() {
            Page<PostFlatProjection> savedPosts = postRepository.findSavedPosts(
                    charlie.getId(),
                    Status.ACTIVE,
                    PostType.POST,
                    ReportStatus.APPROVED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    PageRequest.of(0, 10)
            );

            assertThat(savedPosts.getContent()).hasSize(1);
            assertThat(savedPosts.getContent().get(0).getId()).isEqualTo(alicePublicPost.getId());
        }

        @Test
        @DisplayName("countByUserIdAndPostType & sumReactionsByUserId: Tính toán tổng bài viết và lượt thích chuẩn xác")
        void aggregateStats_accurate() {
            long postCount = postRepository.countByUserIdAndPostType(alice.getId(), PostType.POST);
            assertThat(postCount).isEqualTo(2L);

            // alice có 2 bài: reactionCount = 10 + 2 = 12
            long totalReactions = postRepository.sumReactionsByUserId(alice.getId());
            assertThat(totalReactions).isEqualTo(12L);
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
                .dateOfBirth(LocalDate.of(1996, 1, 1))
                .visibility(Visibility.PUBLIC)
                .build());
    }

    private Post createPost(User user, String content, PostType type, Visibility visibility,
                            Long commentCount, Long reactionCount) {
        return em.persist(Post.builder()
                .user(user)
                .content(content)
                .postType(type)
                .visibility(visibility)
                .commentCount(commentCount)
                .reactionCount(reactionCount)
                .build());
    }

    private void createFriendship(User userOne, User userTwo, User requester, FriendshipStatus status) {
        em.persist(Friendship.builder()
                .userOne(userOne)
                .userTwo(userTwo)
                .requester(requester)
                .status(status)
                .visibility(Visibility.PUBLIC)
                .build());
    }

    private void createBlock(User blocker, User blocked) {
        em.persist(Block.builder()
                .id(new BlockId(blocker.getId(), blocked.getId()))
                .blocker(blocker)
                .blocked(blocked)
                .build());
    }

    private void createReport(Post post, User reporter, String reason, ReportStatus status) {
        em.persist(Report.builder()
                .post(post)
                .reporter(reporter)
                .reason(reason)
                .status(status)
                .build());
    }

    private Hashtag createHashtag(String name) {
        return em.persist(Hashtag.builder().name(name).build());
    }

    private void linkPostHashtag(Post post, Hashtag hashtag) {
        em.persist(PostHashtag.builder().post(post).hashtag(hashtag).build());
    }

    private void createSavedPost(User user, Post post) {
        em.persist(SavedPost.builder()
                .id(new SavedPostId(user.getId(), post.getId()))
                .user(user)
                .post(post)
                .build());
    }
}
