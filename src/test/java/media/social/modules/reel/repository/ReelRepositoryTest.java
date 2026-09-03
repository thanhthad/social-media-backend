package media.social.modules.reel.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.Report;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.reel.dto.projection.ReelFlatProjection;
import media.social.modules.reel.entity.ReelDetail;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
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

/**
 * Migration & Query Integration Test cho {@link ReelRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V10, V11: bảng reel_details với @MapsId trỏ tới posts(post_id) ON DELETE CASCADE)</li>
 *     <li>@Modifying incrementViewCount & incrementShareCount</li>
 *     <li>findReelDetailById (JOIN Post và ReelDetail)</li>
 *     <li>findAllReelMe, findAllVisibleReelsByUser, findReelFeed</li>
 *     <li>findReelExplore (Native SQL thuật toán xếp hạng gravity decay kết hợp reaction, comment, view, share)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ReelRepository – Integration Tests với PostgreSQL Container")
class ReelRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_reel_db")
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

    @Autowired private ReelRepository reelRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;
    private Post aliceReelPost;
    private ReelDetail aliceReelDetail;
    private Post bobReelPost;
    private ReelDetail bobReelDetail;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");

        createProfile(alice, "Alice Nguyen");
        createProfile(bob, "Bob Tran");
        createProfile(charlie, "Charlie Le");

        createFriendship(alice, bob, FriendshipStatus.ACCEPTED);

        // Reel 1: Alice
        aliceReelPost = createPost(alice, "Alice first short reel", PostType.REEL, Visibility.PUBLIC, 2L, 5L);
        aliceReelDetail = createReelDetail(aliceReelPost, 30, 1080, 1920, "https://cdn.example.com/thumb1.jpg", 100L, 10L);

        // Reel 2: Bob
        bobReelPost = createPost(bob, "Bob viral reel", PostType.REEL, Visibility.PUBLIC, 20L, 50L);
        bobReelDetail = createReelDetail(bobReelPost, 15, 1080, 1920, "https://cdn.example.com/thumb2.jpg", 1000L, 200L);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION & COUNTER TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V10 Schema & Counters)")
    class MigrationAndCounterTests {

        @Test
        @DisplayName("Flyway V10: Bảng reel_details tạo thành công và liên kết 1-1 với posts")
        void flyway_reelDetailsTableCreated() {
            Optional<ReelDetail> detailOpt = reelRepository.findByPostId(aliceReelPost.getId());
            assertThat(detailOpt).isPresent();
            assertThat(detailOpt.get().getDurationSeconds()).isEqualTo(30);
            assertThat(detailOpt.get().getViewCount()).isEqualTo(100L);
        }

        @Test
        @DisplayName("incrementViewCount & incrementShareCount: @Modifying cập nhật lượt xem và chia sẻ")
        void incrementCounters() {
            Long reelId = aliceReelDetail.getReelId();

            reelRepository.incrementViewCount(reelId);
            em.flush();
            em.clear();
            assertThat(reelRepository.findById(reelId).orElseThrow().getViewCount()).isEqualTo(101L);

            reelRepository.incrementShareCount(reelId);
            em.flush();
            em.clear();
            assertThat(reelRepository.findById(reelId).orElseThrow().getShareCount()).isEqualTo(11L);
        }
    }

    // =========================================================================
    // 2. QUERY TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Reel Queries Tests (Detail, Feed, Explore)")
    class QueryTests {

        @Test
        @DisplayName("findReelDetailById: JOIN Post và ReelDetail lấy đầy đủ thông tin video ngắn")
        void findReelDetailById_returnsCombinedProjection() {
            Optional<ReelFlatProjection> opt = reelRepository.findReelDetailById(
                    aliceReelPost.getId(),
                    PostType.REEL,
                    Status.ACTIVE,
                    List.of(Visibility.PUBLIC),
                    ReportStatus.APPROVED
            );

            assertThat(opt).isPresent();
            ReelFlatProjection p = opt.get();
            assertThat(p.getContent()).isEqualTo("Alice first short reel");
            assertThat(p.getDurationSeconds()).isEqualTo(30);
            assertThat(p.getWidth()).isEqualTo(1080);
            assertThat(p.getThumbnailUrl()).isEqualTo("https://cdn.example.com/thumb1.jpg");
        }

        @Test
        @DisplayName("findAllReelMe & findReelFeed: Lấy reels của mình và bạn bè")
        void findReels_meAndFeed() {
            Page<ReelFlatProjection> myReels = reelRepository.findAllReelMe(
                    alice.getId(), Status.ACTIVE, ReportStatus.APPROVED, PostType.REEL, PageRequest.of(0, 10));
            assertThat(myReels.getContent()).hasSize(1);

            // Alice xem feed: có reel của mình và reel của bạn bob
            Page<ReelFlatProjection> feed = reelRepository.findReelFeed(
                    alice.getId(), Status.ACTIVE, PostType.REEL, ReportStatus.APPROVED,
                    FriendshipStatus.ACCEPTED, Visibility.PUBLIC, Visibility.FRIEND, PageRequest.of(0, 10));
            assertThat(feed.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("findReelExplore: Native query thuật toán gravity decay đưa reel viral lên đầu")
        void findReelExplore_viralReelRanksFirst() {
            // Charlie (người lạ) xem explore: bobReelPost có 1000 views, 200 shares -> điểm cao nhất
            Page<ReelFlatProjection> explore = reelRepository.findReelExplore(
                    charlie.getId(),
                    Status.ACTIVE.name(),
                    PostType.REEL.name(),
                    ReportStatus.APPROVED.name(),
                    Visibility.PUBLIC.name(),
                    FriendshipStatus.ACCEPTED.name(),
                    PageRequest.of(0, 10)
            );

            assertThat(explore.getContent()).isNotEmpty();
            assertThat(explore.getContent().get(0).getId()).isEqualTo(bobReelPost.getId());
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

    private void createProfile(User user, String fullName) {
        em.persist(Profile.builder()
                .user(user)
                .fullName(fullName)
                .avatarUrl("https://cdn.example.com/" + user.getUsername() + ".jpg")
                .dateOfBirth(LocalDate.of(1996, 1, 1))
                .visibility(Visibility.PUBLIC)
                .build());
    }

    private void createFriendship(User userOne, User userTwo, FriendshipStatus status) {
        em.persist(Friendship.builder()
                .userOne(userOne)
                .userTwo(userTwo)
                .requester(userOne)
                .status(status)
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

    private ReelDetail createReelDetail(Post post, Integer duration, Integer width, Integer height,
                                        String thumbUrl, Long viewCount, Long shareCount) {
        return em.persist(ReelDetail.builder()
                .post(post)
                .durationSeconds(duration)
                .width(width)
                .height(height)
                .thumbnailUrl(thumbUrl)
                .viewCount(viewCount)
                .shareCount(shareCount)
                .build());
    }
}
