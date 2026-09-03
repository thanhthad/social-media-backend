package media.social.modules.user.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.projection.ListUserBlockedProjection;
import media.social.modules.user.entity.Block;
import media.social.modules.user.entity.BlockId;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho {@link BlockRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1 bảng blocks, khóa chính phức hợp BlockId(blocker_id, blocked_id), ON DELETE CASCADE)</li>
 *     <li>existsByBlockerIdAndBlockedId (Derived query kiểm tra trạng thái chặn)</li>
 *     <li>findBlock (JPQL query tìm đối tượng Block cụ thể)</li>
 *     <li>findBlockedUsers (JPQL JOIN b.blocked u LEFT JOIN u.profile p với ORDER BY và Pageable)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("BlockRepository – Integration Tests với PostgreSQL Container")
class BlockRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("social_test_block_db")
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
    private BlockRepository blockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");

        createProfile(alice, "Alice Nguyen", "https://cdn.example.com/alice.jpg");
        createProfile(bob, "Bob Tran", "https://cdn.example.com/bob.jpg");
        // charlie không có profile để test LEFT JOIN trong findBlockedUsers

        // alice block bob
        createBlock(alice, bob);

        // alice block charlie
        createBlock(alice, charlie);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1 Schema, Composite PK, Foreign Key Cascade)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: Khóa chính phức hợp (blocker_id, blocked_id) ngăn chặn duplicate block")
        void flyway_compositePrimaryKey_preventsDuplicate() {
            Block duplicate = Block.builder()
                    .id(new BlockId(alice.getId(), bob.getId()))
                    .blocker(alice)
                    .blocked(bob)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).isNotNull();
        }

        @Test
        @DisplayName("Flyway V1: ON DELETE CASCADE tự động dọn sạch bảng blocks khi User bị xóa")
        void flyway_onDeleteCascade_cleansBlocks() {
            // Xóa bob
            userRepository.deleteById(bob.getId());
            em.flush();
            em.clear();

            // Block giữa alice và bob phải tự biến mất
            boolean exists = blockRepository.existsByBlockerIdAndBlockedId(alice.getId(), bob.getId());
            assertThat(exists).isFalse();
        }
    }

    // =========================================================================
    // 2. QUERY TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Query Tests (exists, findBlock, findBlockedUsers)")
    class QueryTests {

        @Test
        @DisplayName("existsByBlockerIdAndBlockedId: Trả về true khi bị chặn, false khi không")
        void existsByBlockerIdAndBlockedId_accurate() {
            // alice block bob -> true
            assertThat(blockRepository.existsByBlockerIdAndBlockedId(alice.getId(), bob.getId())).isTrue();

            // bob KHÔNG block alice -> false (quan hệ 1 chiều)
            assertThat(blockRepository.existsByBlockerIdAndBlockedId(bob.getId(), alice.getId())).isFalse();
        }

        @Test
        @DisplayName("findBlock: Trả về Entity Block tương ứng")
        void findBlock_returnsEntity() {
            Optional<Block> blockOpt = blockRepository.findBlock(alice.getId(), bob.getId());

            assertThat(blockOpt).isPresent();
            assertThat(blockOpt.get().getBlocker().getId()).isEqualTo(alice.getId());
            assertThat(blockOpt.get().getBlocked().getId()).isEqualTo(bob.getId());
        }

        @Test
        @DisplayName("findBlockedUsers: JOIN b.blocked u LEFT JOIN u.profile p phân trang và ORDER BY")
        void findBlockedUsers_withLeftJoinProfile() {
            Page<ListUserBlockedProjection> page = blockRepository.findBlockedUsers(
                    alice.getId(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);

            // Kiểm tra: charlie không có profile nhưng vẫn có mặt nhờ LEFT JOIN
            List<String> blockedUsernames = page.getContent().stream()
                    .map(ListUserBlockedProjection::getUsername)
                    .toList();
            assertThat(blockedUsernames).containsExactlyInAnyOrder("bob", "charlie");

            // bob có profile -> fullName có dữ liệu
            ListUserBlockedProjection bobProj = page.getContent().stream()
                    .filter(p -> p.getUsername().equals("bob"))
                    .findFirst()
                    .orElseThrow();
            assertThat(bobProj.getFullName()).isEqualTo("Bob Tran");
            assertThat(bobProj.getAvatarUrl()).isEqualTo("https://cdn.example.com/bob.jpg");
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

    private void createBlock(User blocker, User blocked) {
        em.persist(Block.builder()
                .id(new BlockId(blocker.getId(), blocked.getId()))
                .blocker(blocker)
                .blocked(blocked)
                .build());
    }
}
