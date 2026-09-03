package media.social.modules.user.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.*;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho {@link UserRoleRepository}, {@link RoleRepository},
 * và {@link ProfileRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (schema Flyway V1, V2 seed roles USER, ADMIN, MODERATOR)</li>
 *     <li>existsUserRole (CASE WHEN COUNT(ur) > 0 THEN true ELSE false END)</li>
 *     <li>findRolesByUserId (JOIN FETCH ur.role r)</li>
 *     <li>findUsersByRole (JOIN ur.user u)</li>
 *     <li>ProfileRepository (JSONB mapping social_links, CRUD profile)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRole & Profile – Integration Tests với PostgreSQL Container")
class UserRoleAndProfileRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_role_profile_db")
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
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private Role roleAdmin;
    private Role roleUser;
    private Role roleModerator;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        roleAdmin = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        roleUser = roleRepository.findByName(RoleName.USER).orElseThrow();
        roleModerator = roleRepository.findByName(RoleName.MODERATOR).orElseThrow();

        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");

        // alice: USER + ADMIN
        assignRole(alice, roleUser);
        assignRole(alice, roleAdmin);

        // bob: USER
        assignRole(bob, roleUser);

        createProfile(alice, "Alice Nguyen", Map.of("linkedin", "https://linkedin.com/in/alice"));

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1, V2: Roles Seeding & Profile Constraints)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V2: Đã seed sẵn các vai trò hệ thống ADMIN, USER, MODERATOR")
        void flyway_seededRolesExist() {
            assertThat(roleRepository.findByName(RoleName.ADMIN)).isPresent();
            assertThat(roleRepository.findByName(RoleName.USER)).isPresent();
            assertThat(roleRepository.findByName(RoleName.MODERATOR)).isPresent();
        }

        @Test
        @DisplayName("Flyway V1: Khóa chính phức hợp (user_id, role_id) ngăn duplicate gán quyền")
        void flyway_userRoleCompositeKey_preventsDuplicate() {
            UserRole duplicate = UserRole.builder()
                    .id(new UserRoleId(alice.getId(), roleAdmin.getId()))
                    .user(alice)
                    .role(roleAdmin)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).isNotNull();
        }

        @Test
        @DisplayName("Flyway V1: Unique constraint user_id trên bảng profiles bảo đảm quan hệ 1-1")
        void flyway_profileUniqueConstraint_oneToOne() {
            // alice đã có profile trong setUp, thử tạo thêm profile thứ 2 cho alice
            Profile duplicateProfile = Profile.builder()
                    .user(alice)
                    .fullName("Alice Fake")
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicateProfile);
                em.flush();
            }).isNotNull();
        }
    }

    // =========================================================================
    // 2. USER ROLE QUERY TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. UserRoleRepository Queries Test")
    class UserRoleQueriesTests {

        @Test
        @DisplayName("existsUserRole: Query CASE WHEN COUNT(ur) > 0 trả về true/false chính xác")
        void existsUserRole_accurate() {
            assertThat(userRoleRepository.existsUserRole(alice.getId(), RoleName.ADMIN)).isTrue();
            assertThat(userRoleRepository.existsUserRole(alice.getId(), RoleName.USER)).isTrue();
            assertThat(userRoleRepository.existsUserRole(alice.getId(), RoleName.MODERATOR)).isFalse();

            assertThat(userRoleRepository.existsUserRole(bob.getId(), RoleName.ADMIN)).isFalse();
        }

        @Test
        @DisplayName("findRolesByUserId: JPQL JOIN ur.role r trả về đúng danh sách vai trò của user")
        void findRolesByUserId_returnsAllRoles() {
            List<Role> rolesOfAlice = userRoleRepository.findRolesByUserId(alice.getId());

            assertThat(rolesOfAlice).hasSize(2);
            assertThat(rolesOfAlice)
                    .extracting(Role::getName)
                    .containsExactlyInAnyOrder(RoleName.USER, RoleName.ADMIN);
        }

        @Test
        @DisplayName("findUsersByRole: JPQL JOIN ur.user u trả về đúng danh sách users theo vai trò")
        void findUsersByRole_returnsMatchingUsers() {
            List<User> adminUsers = userRoleRepository.findUsersByRole(RoleName.ADMIN);
            assertThat(adminUsers).hasSize(1);
            assertThat(adminUsers.get(0).getUsername()).isEqualTo("alice");

            List<User> regularUsers = userRoleRepository.findUsersByRole(RoleName.USER);
            assertThat(regularUsers).hasSize(2);
            assertThat(regularUsers).extracting(User::getUsername).containsExactlyInAnyOrder("alice", "bob");
        }
    }

    // =========================================================================
    // 3. PROFILE REPOSITORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. ProfileRepository Queries Test")
    class ProfileQueriesTests {

        @Test
        @DisplayName("findByUserId & existsByUserId: Hoạt động chuẩn xác")
        void findByUserId_and_existsByUserId() {
            assertThat(profileRepository.existsByUserId(alice.getId())).isTrue();
            assertThat(profileRepository.existsByUserId(bob.getId())).isFalse();

            Optional<Profile> profileOpt = profileRepository.findByUserId(alice.getId());
            assertThat(profileOpt).isPresent();
            assertThat(profileOpt.get().getFullName()).isEqualTo("Alice Nguyen");
            assertThat(profileOpt.get().getSocialLinks()).containsEntry("linkedin", "https://linkedin.com/in/alice");
        }

        @Test
        @DisplayName("findByUser: Tìm hồ sơ theo thực thể User")
        void findByUser_accurate() {
            Optional<Profile> profileOpt = profileRepository.findByUser(alice);
            assertThat(profileOpt).isPresent();
            assertThat(profileOpt.get().getUser().getId()).isEqualTo(alice.getId());
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

    private void assignRole(User user, Role role) {
        em.persist(UserRole.builder()
                .user(user)
                .role(role)
                .id(new UserRoleId(user.getId(), role.getId()))
                .build());
    }

    private void createProfile(User user, String fullName, Map<String, String> socialLinks) {
        em.persist(Profile.builder()
                .user(user)
                .fullName(fullName)
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .visibility(Visibility.PUBLIC)
                .socialLinks(socialLinks)
                .build());
    }
}
