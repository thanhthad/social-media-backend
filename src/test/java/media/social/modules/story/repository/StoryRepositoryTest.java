package media.social.modules.story.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.story.dto.projection.StoryFeedProjection;
import media.social.modules.story.dto.projection.StoryInteractionProjection;
import media.social.modules.story.dto.projection.UserStoryProjection;
import media.social.modules.story.entity.*;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration & Query Integration Test cho {@link StoryRepository} và {@link StoryViewRepository}
 * sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V9: bảng stories, story_media, story_views, story_reactions)</li>
 *     <li>findActiveById & findActiveByUserId (Lọc story còn hạn expiresAt > now)</li>
 *     <li>findByIdWithUserAndMedia (JOIN FETCH nạp user và media)</li>
 *     <li>findFeed (Subquery lấy story mới nhất của từng người dùng MAX(s2.createdAt), NOT EXISTS Block, EXISTS Friendship)</li>
 *     <li>findInteractionsByStoryIds (JOIN viewer LEFT JOIN profile LEFT JOIN storyReaction)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("StoryRepository – Integration Tests với PostgreSQL Container")
class StoryRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_story_db")
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

    @Autowired private StoryRepository storyRepository;
    @Autowired private StoryViewRepository storyViewRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private Story activeStory;
    private Story expiredStory;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");

        createProfile(alice, "Alice Nguyen");
        createProfile(bob, "Bob Tran");

        createFriendship(alice, bob, FriendshipStatus.ACCEPTED);

        // Active story của bob (còn 12 tiếng)
        activeStory = createStory(bob, "Active story", Visibility.PUBLIC, LocalDateTime.now().plusHours(12));
        createStoryMedia(activeStory, "https://cdn.example.com/story1.jpg", "pub_story_1");

        // Expired story của bob (đã hết hạn 2 tiếng trước)
        expiredStory = createStory(bob, "Expired story", Visibility.PUBLIC, LocalDateTime.now().minusHours(2));
        createStoryMedia(expiredStory, "https://cdn.example.com/story2.jpg", "pub_story_2");

        // Alice xem activeStory
        createStoryView(activeStory, alice);

        // Alice thả tim trên activeStory
        createStoryReaction(activeStory, alice, ReactionType.LOVE);

        em.flush();

        // Cập nhật createdAt và expiresAt trong DB thỏa mãn ck_story_expiration (expires_at > created_at)
        LocalDateTime now = LocalDateTime.now();
        em.getEntityManager().createQuery("UPDATE Story s SET s.createdAt = :created, s.expiresAt = :expired WHERE s.id = :id")
                .setParameter("created", now.minusHours(26))
                .setParameter("expired", now.minusHours(2))
                .setParameter("id", expiredStory.getId())
                .executeUpdate();

        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION & EXPIRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V9 Schema & Expiration Logic)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V9: Bảng stories và story_media tồn tại, tìm active story còn hạn")
        void flyway_storyTablesReady_findActive() {
            LocalDateTime now = LocalDateTime.now();

            Optional<Story> activeOpt = storyRepository.findActiveById(activeStory.getId(), now);
            assertThat(activeOpt).isPresent();

            // Expired story không tìm thấy
            Optional<Story> expiredOpt = storyRepository.findActiveById(expiredStory.getId(), now);
            assertThat(expiredOpt).isEmpty();
        }

        @Test
        @DisplayName("findByIdWithUserAndMedia: JOIN FETCH nạp đầy đủ User và Media")
        void findByIdWithUserAndMedia_eagerlyLoaded() {
            Optional<Story> opt = storyRepository.findByIdWithUserAndMedia(activeStory.getId());

            assertThat(opt).isPresent();
            Story s = opt.get();
            assertThat(s.getUser().getUsername()).isEqualTo("bob");
            assertThat(s.getMedia()).isNotNull();
            assertThat(s.getMedia().getUrl()).isEqualTo("https://cdn.example.com/story1.jpg");
        }
    }

    // =========================================================================
    // 2. STORY FEED & USER STORIES TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Story Feed & Interactions Queries")
    class StoryFeedAndInteractionsTests {

        @Test
        @DisplayName("findFeed: Lấy story mới nhất của từng user, loại trừ story hết hạn")
        void findFeed_returnsLatestActiveStoryPerUser() {
            LocalDateTime now = LocalDateTime.now();

            List<StoryFeedProjection> feed = storyRepository.findFeed(
                    alice.getId(),
                    now,
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    Status.ACTIVE
            );

            assertThat(feed).hasSize(1);
            StoryFeedProjection p = feed.get(0);
            assertThat(p.getUserId()).isEqualTo(bob.getId());
            assertThat(p.getUrl()).isEqualTo("https://cdn.example.com/story1.jpg");
        }

        @Test
        @DisplayName("findUserStories: Lấy danh sách các story còn hạn của 1 user cụ thể")
        void findUserStories_returnsActiveOnly() {
            LocalDateTime now = LocalDateTime.now();

            List<UserStoryProjection> stories = storyRepository.findUserStories(
                    alice.getId(),
                    bob.getId(),
                    now,
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    Status.ACTIVE
            );

            // Chỉ có 1 activeStory còn hạn
            assertThat(stories).hasSize(1);
            assertThat(stories.get(0).getContent()).isEqualTo("Active story");
        }

        @Test
        @DisplayName("findInteractionsByStoryIds: Lấy lịch sử xem và cảm xúc trên story")
        void findInteractionsByStoryIds_accurate() {
            List<StoryInteractionProjection> interactions =
                    storyViewRepository.findInteractionsByStoryIds(List.of(activeStory.getId()));

            assertThat(interactions).hasSize(1);
            StoryInteractionProjection inter = interactions.get(0);
            assertThat(inter.getUsername()).isEqualTo("alice");
            assertThat(inter.getReactionType()).isEqualTo(ReactionType.LOVE);
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

    private Story createStory(User user, String content, Visibility visibility, LocalDateTime expiresAt) {
        Story story = Story.builder()
                .user(user)
                .content(content)
                .visibility(visibility)
                .expiresAt(expiresAt)
                .build();
        return em.persist(story);
    }

    private void createStoryMedia(Story story, String url, String publicId) {
        StoryMedia media = StoryMedia.builder()
                .story(story)
                .url(url)
                .publicId(publicId)
                .mediaType(MediaType.IMAGE)
                .build();
        em.persist(media);
        story.setMedia(media);
    }

    private void createStoryView(Story story, User viewer) {
        em.persist(StoryView.builder()
                .id(new StoryViewId(story.getId(), viewer.getId()))
                .story(story)
                .viewer(viewer)
                .viewedAt(LocalDateTime.now())
                .build());
    }

    private void createStoryReaction(Story story, User user, ReactionType type) {
        em.persist(StoryReaction.builder()
                .story(story)
                .user(user)
                .type(type)
                .build());
    }
}
