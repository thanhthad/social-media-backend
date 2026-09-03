package media.social.modules.notification.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.notification.dto.projection.NotificationProjection;
import media.social.modules.notification.entity.Notification;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration & Query Integration Test cho {@link NotificationRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1: bảng notifications, foreign keys receiver_id, sender_id)</li>
 *     <li>findMyNotifications & findMyUnreadNotifications (JOIN sender LEFT JOIN profile, ORDER BY createdAt DESC, Pageable)</li>
 *     <li>countByReceiver_IdAndIsReadFalse (Đếm số thông báo chưa đọc)</li>
 *     <li>@Modifying markAllAsRead (Đánh dấu toàn bộ thông báo của user thành đã đọc)</li>
 *     <li>exists & delete theo receiver, sender, entityType, entityId, type</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("NotificationRepository – Integration Tests với PostgreSQL Container")
class NotificationRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_noti_db")
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

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");

        createProfile(alice, "Alice Nguyen");
        createProfile(bob, "Bob Tran");
        createProfile(charlie, "Charlie Le");

        // Notification 1: bob like post của alice (unread)
        createNotification(alice, bob, EntityType.POST, 100L, NotificationType.POST_REACTION, false, LocalDateTime.now().minusMinutes(10));

        // Notification 2: charlie comment post của alice (unread)
        createNotification(alice, charlie, EntityType.POST, 100L, NotificationType.POST_COMMENT, false, LocalDateTime.now().minusMinutes(5));

        // Notification 3: bob send friend request tới alice (read)
        createNotification(alice, bob, EntityType.FRIENDSHIP, 200L, NotificationType.FRIEND_REQUEST, true, LocalDateTime.now().minusHours(1));

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1 Schema)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: Bảng notifications được tạo thành công")
        void flyway_notificationsTableExists() {
            assertThat(notificationRepository.count()).isEqualTo(3L);
        }
    }

    // =========================================================================
    // 2. QUERY TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Notification Queries Test")
    class QueryTests {

        @Test
        @DisplayName("findMyNotifications: Lấy tất cả thông báo của receiver kèm profile sender và phân trang")
        void findMyNotifications_returnsAll() {
            Page<NotificationProjection> page = notificationRepository.findMyNotifications(
                    alice.getId(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(3);
            // Sắp xếp createdAt DESC: notification 2 (5 mins ago) trước notification 1 (10 mins ago)
            assertThat(page.getContent().get(0).getSenderUsername()).isEqualTo("charlie");
            assertThat(page.getContent().get(0).getType()).isEqualTo(NotificationType.POST_COMMENT);
        }

        @Test
        @DisplayName("findMyUnreadNotifications & countByReceiver_IdAndIsReadFalse: Chỉ lấy thông báo chưa đọc")
        void unreadNotifications_accurate() {
            long unreadCount = notificationRepository.countByReceiver_IdAndIsReadFalse(alice.getId());
            assertThat(unreadCount).isEqualTo(2L);

            Page<NotificationProjection> unreadPage = notificationRepository.findMyUnreadNotifications(
                    alice.getId(), PageRequest.of(0, 10));

            assertThat(unreadPage.getContent()).hasSize(2);
            assertThat(unreadPage.getContent())
                    .allMatch(n -> !n.getIsRead());
        }

        @Test
        @DisplayName("markAllAsRead: @Modifying cập nhật toàn bộ thông báo chưa đọc thành đã đọc")
        void markAllAsRead_updatesAllToRead() {
            notificationRepository.markAllAsRead(alice.getId());
            em.flush();
            em.clear();

            long unreadAfter = notificationRepository.countByReceiver_IdAndIsReadFalse(alice.getId());
            assertThat(unreadAfter).isEqualTo(0L);
        }

        @Test
        @DisplayName("exists & delete theo receiver, sender, entityType, entityId, type")
        void existsAndDelete_uniqueNotification() {
            boolean exists = notificationRepository.existsByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                    alice.getId(), bob.getId(), EntityType.POST, 100L, NotificationType.POST_REACTION);
            assertThat(exists).isTrue();

            notificationRepository.deleteByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                    alice.getId(), bob.getId(), EntityType.POST, 100L, NotificationType.POST_REACTION);
            em.flush();
            em.clear();

            boolean existsAfter = notificationRepository.existsByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                    alice.getId(), bob.getId(), EntityType.POST, 100L, NotificationType.POST_REACTION);
            assertThat(existsAfter).isFalse();
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
                .avatarUrl("https://avatar.com/" + user.getUsername() + ".png")
                .dateOfBirth(LocalDate.of(1996, 1, 1))
                .build());
    }

    private void createNotification(User receiver, User sender, EntityType entityType,
                                    Long entityId, NotificationType type, boolean isRead, LocalDateTime createdAt) {
        em.persist(Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .entityType(entityType)
                .entityId(entityId)
                .type(type)
                .isRead(isRead)
                .createdAt(createdAt)
                .build());
    }
}
