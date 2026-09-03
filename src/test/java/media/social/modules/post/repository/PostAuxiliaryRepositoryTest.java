package media.social.modules.post.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.ListPostMediaProjection;
import media.social.modules.post.dto.projection.PostMediaProjection;
import media.social.modules.post.dto.projection.ReportDetailProjection;
import media.social.modules.post.dto.projection.TrendingHashtagProjection;
import media.social.modules.post.entity.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
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
 * Migration & Query Integration Test cho các Repository phụ trợ của Post:
 * {@link SavedPostRepository}, {@link HashtagRepository}, {@link PostHashtagRepository},
 * {@link ReportRepository}, {@link PostMediaRepository}.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1: uk_report_user_post, uk_post_hashtag, saved_posts PK)</li>
 *     <li>Hashtag: deleteIfUnused với NOT EXISTS subquery</li>
 *     <li>PostHashtag: getTrendingHashtags với GROUP BY và COUNT(ph) DESC</li>
 *     <li>Report: getAll, getByStatus với multi-joins (post, reporter, profile, reviewer)</li>
 *     <li>PostMedia: findMediaByPostId, findMediaByPostIds</li>
 *     <li>SavedPost: exists, delete</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Post Auxiliary Repositories – Integration Tests với PostgreSQL Container")
class PostAuxiliaryRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_post_aux_db")
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

    @Autowired private SavedPostRepository savedPostRepository;
    @Autowired private HashtagRepository hashtagRepository;
    @Autowired private PostHashtagRepository postHashtagRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private PostMediaRepository postMediaRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private User admin;
    private Post post1;
    private Post post2;
    private Hashtag tagJava;
    private Hashtag tagSpring;
    private Hashtag tagUnused;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        admin = createUser("admin", "admin@example.com");

        createProfile(alice, "Alice Nguyen");
        createProfile(bob, "Bob Tran");

        post1 = createPost(alice, "Post 1 with #java and #spring");
        post2 = createPost(bob, "Post 2 with #java");

        tagJava = createHashtag("java");
        tagSpring = createHashtag("spring");
        tagUnused = createHashtag("unused_tag");

        linkPostHashtag(post1, tagJava);
        linkPostHashtag(post1, tagSpring);
        linkPostHashtag(post2, tagJava);

        // Media on post1
        createPostMedia(post1, "https://cloudinary.com/img1.jpg", "pub_img_1", MediaType.IMAGE);
        createPostMedia(post1, "https://cloudinary.com/vid1.mp4", "pub_vid_1", MediaType.VIDEO);

        // Saved post
        createSavedPost(bob, post1);

        // Report on post1 by bob, reviewed by admin
        createReport(post1, bob, admin, "Spam detected", ReportStatus.APPROVED);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS (Database Constraints)
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Constraints & Primary Keys)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: uk_report_user_post ngăn 1 user báo cáo trùng 1 bài viết")
        void flyway_uniqueReportConstraint() {
            Report duplicate = Report.builder()
                    .post(post1)
                    .reporter(bob)
                    .reason("Duplicate report")
                    .status(ReportStatus.PENDING)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_report_user_post");
        }

        @Test
        @DisplayName("Flyway V1: uk_post_hashtag ngăn gán trùng cùng 1 hashtag vào 1 bài viết")
        void flyway_uniquePostHashtagConstraint() {
            PostHashtag duplicate = PostHashtag.builder()
                    .post(post1)
                    .hashtag(tagJava)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_post_hashtag");
        }
    }

    // =========================================================================
    // 2. HASHTAG & TRENDING TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Hashtag & Trending Queries Test")
    class HashtagQueriesTests {

        @Test
        @DisplayName("deleteIfUnused: Xóa hashtag khi không có PostHashtag nào trỏ tới (NOT EXISTS)")
        void deleteIfUnused_removesOrphanTag() {
            // tagUnused không thuộc bài viết nào -> bị xóa
            hashtagRepository.deleteIfUnused(tagUnused.getHashtagId());
            em.flush();
            em.clear();
            assertThat(hashtagRepository.findById(tagUnused.getHashtagId())).isEmpty();

            // tagJava đang gắn với post1 và post2 -> KHÔNG bị xóa nhờ NOT EXISTS
            hashtagRepository.deleteIfUnused(tagJava.getHashtagId());
            em.flush();
            em.clear();
            assertThat(hashtagRepository.findById(tagJava.getHashtagId())).isPresent();
        }

        @Test
        @DisplayName("getTrendingHashtags: GROUP BY hashtag, COUNT(ph) DESC")
        void getTrendingHashtags_orderedByUsage() {
            // tagJava gắn vào 2 post, tagSpring gắn vào 1 post
            List<TrendingHashtagProjection> trending =
                    postHashtagRepository.getTrendingHashtags(PageRequest.of(0, 10));

            assertThat(trending).hasSize(2);
            assertThat(trending.get(0).getName()).isEqualTo("java");
            assertThat(trending.get(0).getTotalPosts()).isEqualTo(2L);

            assertThat(trending.get(1).getName()).isEqualTo("spring");
            assertThat(trending.get(1).getTotalPosts()).isEqualTo(1L);
        }

        @Test
        @DisplayName("findByNameStartingWithIgnoreCase: Tìm kiếm autocomplete hashtag")
        void findByNameStartingWithIgnoreCase_autocomplete() {
            List<Hashtag> results = hashtagRepository.findByNameStartingWithIgnoreCase("SP");
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("spring");
        }
    }

    // =========================================================================
    // 3. REPORT REPOSITORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. ReportRepository Queries Test")
    class ReportQueriesTests {

        @Test
        @DisplayName("getAll & getByStatus: JOIN post, reporter, profile, reviewer trả về đầy đủ projection")
        void getReports_withAllJoinedEntities() {
            Page<ReportDetailProjection> allReports = reportRepository.getAll(PageRequest.of(0, 10));
            assertThat(allReports.getContent()).hasSize(1);

            ReportDetailProjection detail = allReports.getContent().get(0);
            assertThat(detail.getPostId()).isEqualTo(post1.getId());
            assertThat(detail.getReporterUsername()).isEqualTo("bob");
            assertThat(detail.getReviewedByUsername()).isEqualTo("admin");
            assertThat(detail.getStatus()).isEqualTo(ReportStatus.APPROVED);

            // getByStatus lọc đúng status
            Page<ReportDetailProjection> approved = reportRepository.getByStatus(ReportStatus.APPROVED, PageRequest.of(0, 10));
            assertThat(approved.getContent()).hasSize(1);

            Page<ReportDetailProjection> pending = reportRepository.getByStatus(ReportStatus.PENDING, PageRequest.of(0, 10));
            assertThat(pending.getContent()).isEmpty();
        }

        @Test
        @DisplayName("existsByReporter_IdAndPost_Id: Kiểm tra trạng thái đã báo cáo")
        void existsByReporterAndPost() {
            assertThat(reportRepository.existsByReporter_IdAndPost_Id(bob.getId(), post1.getId())).isTrue();
            assertThat(reportRepository.existsByReporter_IdAndPost_Id(alice.getId(), post1.getId())).isFalse();
        }
    }

    // =========================================================================
    // 4. POST MEDIA & SAVED POST TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. PostMedia & SavedPost Queries Test")
    class MediaAndSavedPostTests {

        @Test
        @DisplayName("findMediaByPostId & findMediaByPostIds: Lấy danh sách media của post")
        void postMedia_queries() {
            List<PostMediaProjection> medias = postMediaRepository.findMediaByPostId(post1.getId());
            assertThat(medias).hasSize(2);
            assertThat(medias).extracting(PostMediaProjection::getType)
                    .containsExactlyInAnyOrder(MediaType.IMAGE, MediaType.VIDEO);

            List<ListPostMediaProjection> batchMedias = postMediaRepository.findMediaByPostIds(List.of(post1.getId(), post2.getId()));
            assertThat(batchMedias).hasSize(2);
        }

        @Test
        @DisplayName("SavedPost: exists và delete hoạt động đúng")
        void savedPost_crud() {
            assertThat(savedPostRepository.existsByUser_IdAndPost_Id(bob.getId(), post1.getId())).isTrue();

            savedPostRepository.deleteByUser_IdAndPost_Id(bob.getId(), post1.getId());
            em.flush();
            em.clear();

            assertThat(savedPostRepository.existsByUser_IdAndPost_Id(bob.getId(), post1.getId())).isFalse();
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

    private Hashtag createHashtag(String name) {
        return em.persist(Hashtag.builder().name(name).build());
    }

    private void linkPostHashtag(Post post, Hashtag hashtag) {
        em.persist(PostHashtag.builder().post(post).hashtag(hashtag).build());
    }

    private void createPostMedia(Post post, String url, String publicId, MediaType type) {
        em.persist(PostMedia.builder()
                .post(post)
                .url(url)
                .publicId(publicId)
                .mediaType(type)
                .build());
    }

    private void createSavedPost(User user, Post post) {
        em.persist(SavedPost.builder()
                .id(new SavedPostId(user.getId(), post.getId()))
                .user(user)
                .post(post)
                .build());
    }

    private void createReport(Post post, User reporter, User reviewer, String reason, ReportStatus status) {
        em.persist(Report.builder()
                .post(post)
                .reporter(reporter)
                .reviewedBy(reviewer)
                .reason(reason)
                .status(status)
                .build());
    }
}
