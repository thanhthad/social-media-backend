package media.social.modules.user.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.projection.AdminUserProjection;
import media.social.modules.user.dto.projection.PublicUserProfileCacheProjection;
import media.social.modules.user.dto.projection.UserSearchProjection;
import media.social.modules.user.entity.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration & Query Integration Test cho {@link UserRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử các query thực tế có sẵn trong UserRepository:
 * <ul>
 *     <li>Migration test (Flyway V1..V11, bảng users, profiles, extension pg_trgm, JSONB mapping)</li>
 *     <li>JOIN (findCurrentUserProfileCache: JPQL INNER JOIN với Profile projection)</li>
 *     <li>LEFT JOIN / LEFT JOIN FETCH (findByEmailWithRoles, findByIdWithProfile, findAllAdminUsers)</li>
 *     <li>NOT EXISTS (searchUsers: native SQL subquery loại bỏ blocked user cả 2 chiều)</li>
 *     <li>ORDER BY (findAllAdminUsers: u.createdAt DESC; searchUsers: similarity DESC)</li>
 *     <li>nativeQuery & similarity() (searchUsers, searchAdminUsers dùng hàm pg_trgm)</li>
 *     <li>Pagination (Pageable, PageRequest, totalElements, totalPages, countQuery riêng)</li>
 *     <li>Complex WHERE (similarity > 0.3 AND status = :status AND NOT EXISTS(...), (:status IS NULL OR u.status = :status))</li>
 *     <li>Derived queries (existsByUsername, existsByEmail, findByUsername, findByEmail)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository – Integration Tests với PostgreSQL Testcontainers")
class UserRepositoryTest {

    // =========================================================================
    // 1. TESTCONTAINER CONFIGURATION (Shared PostgreSQL 16 Instance)
    // =========================================================================

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("social_test_user_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Flyway migration: kích hoạt tự động apply V1..V11 vào container
        registry.add("spring.flyway.enabled", () -> "true");
        // Hibernate ddl-auto validate đối chiếu entity mapping với Flyway schema
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // Mock config redis không cần thiết cho repository test
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    // =========================================================================
    // 2. DEPENDENCY INJECTION
    // =========================================================================

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private TestEntityManager em;

    // =========================================================================
    // 3. FIXTURES
    // =========================================================================

    private Role roleUser;
    private Role roleAdmin;

    private User alice;    // ACTIVE, có profile, roles: USER + ADMIN
    private User bob;      // ACTIVE, có profile, roles: USER
    private User charlie;  // ACTIVE, có profile, roles: USER
    private User diana;    // BANNED, KHÔNG CÓ profile (dùng test INNER vs LEFT JOIN)
    private User eve;      // ACTIVE, có profile – bị alice block (dùng test NOT EXISTS)

    @BeforeEach
    void setUp() {
        roleUser = findOrCreateRole(RoleName.USER, "Regular User");
        roleAdmin = findOrCreateRole(RoleName.ADMIN, "System Admin");

        alice = createUser("alice", "alice@example.com", Status.ACTIVE);
        bob = createUser("bob", "bob@example.com", Status.ACTIVE);
        charlie = createUser("charlie", "charlie@example.com", Status.ACTIVE);
        diana = createUser("diana_banned", "diana@example.com", Status.BANNED);
        eve = createUser("eve", "eve@example.com", Status.ACTIVE);

        createProfile(alice, "Alice Nguyen", "Hà Nội", "Software Engineer");
        createProfile(bob, "Bob Tran", "Hồ Chí Minh", "UI/UX Designer");
        createProfile(charlie, "Charlie Le", "Đà Nẵng", "Product Manager");
        // diana cố ý không có profile để test INNER JOIN vs LEFT JOIN
        createProfile(eve, "Eve Pham", "Hải Phòng", "QA Lead");

        assignRole(alice, roleUser);
        assignRole(alice, roleAdmin);
        assignRole(bob, roleUser);
        assignRole(charlie, roleUser);
        assignRole(diana, roleUser);
        assignRole(eve, roleUser);

        // alice block eve để test NOT EXISTS subquery trong searchUsers
        createBlock(alice, eve);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 4. MIGRATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway & PostgreSQL Extensions)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1..V11: Schema và bảng users được khởi tạo thành công trên container")
        void flyway_schemaInitialized_usersTableReady() {
            User testUser = userRepository.save(User.builder()
                    .username("mig_checker")
                    .email("mig_checker@example.com")
                    .status(Status.ACTIVE)
                    .provider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .build());

            assertThat(testUser.getId()).isNotNull();
            assertThat(userRepository.existsById(testUser.getId())).isTrue();
        }

        @Test
        @DisplayName("Flyway V5: Extension pg_trgm đã được kích hoạt, similarity() hoạt động chuẩn")
        void flyway_pgTrgmExtensionActive() {
            // Gọi native query dùng similarity(), nếu extension chưa enable sẽ văng SQLException
            Page<UserSearchProjection> page = userRepository.searchUsers(
                    "alice", Status.ACTIVE.name(), 9999L, PageRequest.of(0, 10));

            assertThat(page).isNotNull();
            assertThat(page.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Flyway V1: Cột social_links kiểu JSONB trên profiles map chuẩn sang Java Map")
        void flyway_jsonbColumnMapping() {
            Optional<PublicUserProfileCacheProjection> profileCache =
                    userRepository.findCurrentUserProfileCache(alice.getId());

            assertThat(profileCache).isPresent();
            assertThat(profileCache.get().getSocialLinks()).isNotNull();
            assertThat(profileCache.get().getSocialLinks()).containsEntry("github", "https://github.com/alice");
        }
    }

    // =========================================================================
    // 5. JOIN TESTS (INNER JOIN)
    // =========================================================================

    @Nested
    @DisplayName("2. JOIN Test (findCurrentUserProfileCache - INNER JOIN)")
    class InnerJoinTests {

        @Test
        @DisplayName("JOIN: Trả về projection đầy đủ khi user có profile")
        void findCurrentUserProfileCache_userWithProfile_returnsProjection() {
            Optional<PublicUserProfileCacheProjection> projection =
                    userRepository.findCurrentUserProfileCache(alice.getId());

            assertThat(projection).isPresent();
            PublicUserProfileCacheProjection p = projection.get();
            assertThat(p.getId()).isEqualTo(alice.getId());
            assertThat(p.getUsername()).isEqualTo("alice");
            assertThat(p.getEmail()).isEqualTo("alice@example.com");
            assertThat(p.getFullName()).isEqualTo("Alice Nguyen");
            assertThat(p.getCity()).isEqualTo("Hà Nội");
            assertThat(p.getOccupation()).isEqualTo("Software Engineer");
        }

        @Test
        @DisplayName("JOIN: INNER JOIN loại bỏ user không có profile (diana -> empty)")
        void findCurrentUserProfileCache_userWithoutProfile_returnsEmpty() {
            Optional<PublicUserProfileCacheProjection> projection =
                    userRepository.findCurrentUserProfileCache(diana.getId());

            assertThat(projection).isEmpty();
        }
    }

    // =========================================================================
    // 6. LEFT JOIN TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. LEFT JOIN / LEFT JOIN FETCH Test")
    class LeftJoinTests {

        @Test
        @DisplayName("LEFT JOIN FETCH: findByEmailWithRoles lấy trọn vẹn roles tránh N+1")
        void findByEmailWithRoles_loadsUserWithAllRoles() {
            Optional<User> userOpt = userRepository.findByEmailWithRoles("alice@example.com");

            assertThat(userOpt).isPresent();
            User user = userOpt.get();
            assertThat(user.getUserRoles()).hasSize(2);
            assertThat(user.getUserRoles())
                    .extracting(ur -> ur.getRole().getName())
                    .containsExactlyInAnyOrder(RoleName.USER, RoleName.ADMIN);
        }

        @Test
        @DisplayName("LEFT JOIN FETCH: findByIdWithProfile nạp sẵn profile")
        void findByIdWithProfile_loadsProfileEagerly() {
            Optional<User> userOpt = userRepository.findByIdWithProfile(bob.getId());

            assertThat(userOpt).isPresent();
            assertThat(userOpt.get().getProfile()).isNotNull();
            assertThat(userOpt.get().getProfile().getFullName()).isEqualTo("Bob Tran");
        }

        @Test
        @DisplayName("LEFT JOIN: findAllAdminUsers vẫn trả về user chưa có profile (diana)")
        void findAllAdminUsers_userWithoutProfile_stillIncluded() {
            Page<AdminUserProjection> result = userRepository.findAllAdminUsers(
                    null, PageRequest.of(0, 50));

            List<Long> userIds = result.getContent().stream()
                    .map(AdminUserProjection::getId)
                    .toList();

            assertThat(userIds).contains(diana.getId());
        }
    }

    // =========================================================================
    // 7. NOT EXISTS TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. NOT EXISTS Test (searchUsers exclude blocked users)")
    class NotExistsTests {

        @Test
        @DisplayName("NOT EXISTS: Loại bỏ user bị viewer block (alice block eve -> eve bị ẩn)")
        void searchUsers_notExists_excludesBlockedUser() {
            Page<UserSearchProjection> page = userRepository.searchUsers(
                    "eve", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 10));

            List<Long> resultIds = page.getContent().stream()
                    .map(UserSearchProjection::getId)
                    .toList();

            assertThat(resultIds).doesNotContain(eve.getId());
        }

        @Test
        @DisplayName("NOT EXISTS: Cho phép viewer khác (bob) thấy eve vì bob không block eve")
        void searchUsers_notExists_allowsNonBlockedViewer() {
            Page<UserSearchProjection> page = userRepository.searchUsers(
                    "eve", Status.ACTIVE.name(), bob.getId(), PageRequest.of(0, 10));

            List<Long> resultIds = page.getContent().stream()
                    .map(UserSearchProjection::getId)
                    .toList();

            assertThat(resultIds).contains(eve.getId());
        }

        @Test
        @DisplayName("NOT EXISTS: Block hai chiều (charlie block bob -> bob search không thấy charlie)")
        void searchUsers_notExists_bidirectionalBlock() {
            createBlock(charlie, bob);
            em.flush();
            em.clear();

            Page<UserSearchProjection> page = userRepository.searchUsers(
                    "charlie", Status.ACTIVE.name(), bob.getId(), PageRequest.of(0, 10));

            List<Long> resultIds = page.getContent().stream()
                    .map(UserSearchProjection::getId)
                    .toList();

            assertThat(resultIds).doesNotContain(charlie.getId());
        }
    }

    // =========================================================================
    // 8. ORDER BY TESTS
    // =========================================================================

    @Nested
    @DisplayName("5. ORDER BY Test")
    class OrderByTests {

        @Test
        @DisplayName("findAllAdminUsers: ORDER BY u.createdAt DESC đúng thứ tự thời gian")
        void findAllAdminUsers_orderedByCreatedAtDesc() {
            Page<AdminUserProjection> page = userRepository.findAllAdminUsers(
                    null, PageRequest.of(0, 50));

            List<LocalDateTime> createdDates = page.getContent().stream()
                    .map(AdminUserProjection::getCreatedAt)
                    .filter(d -> d != null)
                    .toList();

            for (int i = 0; i < createdDates.size() - 1; i++) {
                assertThat(createdDates.get(i)).isAfterOrEqualTo(createdDates.get(i + 1));
            }
        }

        @Test
        @DisplayName("searchAdminUsers: ORDER BY similarity() DESC đưa user khớp nhất lên đầu")
        void searchAdminUsers_orderedBySimilarityDesc() {
            Page<AdminUserProjection> page = userRepository.searchAdminUsers(
                    "alic", PageRequest.of(0, 10));

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent().get(0).getUsername()).isEqualTo("alice");
        }
    }

    // =========================================================================
    // 9. NATIVE QUERY & SIMILARITY() TESTS
    // =========================================================================

    @Nested
    @DisplayName("6. Native Query & similarity() (pg_trgm) Test")
    class NativeQuerySimilarityTests {

        @Test
        @DisplayName("searchUsers: Native similarity > 0.3 tìm kiếm fuzzy ('bobb' khớp 'bob')")
        void searchUsers_fuzzyMatching() {
            Page<UserSearchProjection> result = userRepository.searchUsers(
                    "bobb", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("bob");
        }

        @Test
        @DisplayName("searchUsers: Từ khóa khác biệt hoàn toàn ('zzzzzz') cho similarity < 0.3 -> rỗng")
        void searchUsers_lowSimilarity_returnsEmpty() {
            Page<UserSearchProjection> result = userRepository.searchUsers(
                    "zzzzzz", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("searchAdminUsers: Native query trả về đúng Projection có avatar_url từ profiles")
        void searchAdminUsers_returnsProjectionWithJoinedData() {
            Page<AdminUserProjection> page = userRepository.searchAdminUsers(
                    "charlie", PageRequest.of(0, 10));

            assertThat(page.getContent()).isNotEmpty();
            AdminUserProjection p = page.getContent().get(0);
            assertThat(p.getUsername()).isEqualTo("charlie");
            assertThat(p.getStatus()).isEqualTo(Status.ACTIVE);
            assertThat(p.getAvatarUrl()).isEqualTo("https://avatar.com/charlie.png");
        }
    }

    // =========================================================================
    // 10. PAGINATION & COUNT TESTS
    // =========================================================================

    @Nested
    @DisplayName("7. Pagination & COUNT Test")
    class PaginationAndCountTests {

        @Test
        @DisplayName("findAllAdminUsers: Phân trang pageSize = 2 chia đúng số trang và tổng phần tử")
        void findAllAdminUsers_paginationCalculation() {
            Page<AdminUserProjection> page0 = userRepository.findAllAdminUsers(
                    null, PageRequest.of(0, 2));

            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getNumber()).isEqualTo(0);
            assertThat(page0.getTotalElements()).isGreaterThanOrEqualTo(5L);
            assertThat(page0.getTotalPages()).isEqualTo((int) Math.ceil(page0.getTotalElements() / 2.0));
        }

        @Test
        @DisplayName("searchUsers countQuery: COUNT(*) hoạt động chính xác trong Pageable native query")
        void searchUsers_countQuery_accurate() {
            Page<UserSearchProjection> page = userRepository.searchUsers(
                    "alice", Status.ACTIVE.name(), bob.getId(), PageRequest.of(0, 1));

            assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1L);
            assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("searchUsers: Dữ liệu ở trang 0 và trang 1 không bị trùng lặp phần tử")
        void searchUsers_noOverlapBetweenPages() {
            for (int i = 1; i <= 5; i++) {
                User u = createUser("testuser_" + i, "tu" + i + "@example.com", Status.ACTIVE);
                createProfile(u, "Test User " + i, "Hà Nội", "Tester");
            }
            em.flush();
            em.clear();

            Page<UserSearchProjection> page0 = userRepository.searchUsers(
                    "testuser", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 2));
            Page<UserSearchProjection> page1 = userRepository.searchUsers(
                    "testuser", Status.ACTIVE.name(), alice.getId(), PageRequest.of(1, 2));

            List<Long> idsPage0 = page0.getContent().stream().map(UserSearchProjection::getId).toList();
            List<Long> idsPage1 = page1.getContent().stream().map(UserSearchProjection::getId).toList();

            assertThat(idsPage0).hasSize(2);
            assertThat(idsPage1).hasSize(2);
            assertThat(idsPage0).doesNotContainAnyElementsOf(idsPage1);
        }
    }

    // =========================================================================
    // 11. COMPLEX WHERE TESTS
    // =========================================================================

    @Nested
    @DisplayName("8. Complex WHERE Multi-Condition Test")
    class ComplexWhereTests {

        @Test
        @DisplayName("findAllAdminUsers: status = null trả về TẤT CẢ trạng thái (nullable condition)")
        void findAllAdminUsers_nullStatus_returnsAll() {
            Page<AdminUserProjection> page = userRepository.findAllAdminUsers(
                    null, PageRequest.of(0, 100));

            List<Status> statuses = page.getContent().stream()
                    .map(AdminUserProjection::getStatus)
                    .toList();

            assertThat(statuses).contains(Status.ACTIVE, Status.BANNED);
        }

        @Test
        @DisplayName("findAllAdminUsers: status = BANNED chỉ lọc ra user bị khóa")
        void findAllAdminUsers_specificStatus_returnsFiltered() {
            Page<AdminUserProjection> page = userRepository.findAllAdminUsers(
                    Status.BANNED, PageRequest.of(0, 100));

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent())
                    .allMatch(u -> u.getStatus() == Status.BANNED);
        }

        @Test
        @DisplayName("searchUsers: Kết hợp đồng thời similarity > 0.3, status = ACTIVE và NOT EXISTS")
        void searchUsers_allThreeConditionsCombined() {
            // diana có username tương đồng nhưng status = BANNED -> bị loại
            Page<UserSearchProjection> dianaSearch = userRepository.searchUsers(
                    "diana", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 10));
            assertThat(dianaSearch.getContent()).isEmpty();

            // eve có status = ACTIVE, similarity cao nhưng bị alice block -> bị loại
            Page<UserSearchProjection> eveSearch = userRepository.searchUsers(
                    "eve", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 10));
            assertThat(eveSearch.getContent()).isEmpty();

            // charlie có status = ACTIVE, similarity cao, KHÔNG bị block -> thành công
            Page<UserSearchProjection> charlieSearch = userRepository.searchUsers(
                    "charlie", Status.ACTIVE.name(), alice.getId(), PageRequest.of(0, 10));
            assertThat(charlieSearch.getContent()).isNotEmpty();
            assertThat(charlieSearch.getContent().get(0).getUsername()).isEqualTo("charlie");
        }
    }

    // =========================================================================
    // 12. DERIVED & BASIC CRUD TESTS
    // =========================================================================

    @Nested
    @DisplayName("9. Derived Queries (exists, findBy...)")
    class DerivedQueriesTests {

        @Test
        @DisplayName("existsByUsername: Trả về true khi tồn tại, false khi không")
        void existsByUsername_works() {
            assertThat(userRepository.existsByUsername("alice")).isTrue();
            assertThat(userRepository.existsByUsername("non_existent")).isFalse();
        }

        @Test
        @DisplayName("existsByEmail: Trả về true khi tồn tại, false khi không")
        void existsByEmail_works() {
            assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
            assertThat(userRepository.existsByEmail("non_existent@example.com")).isFalse();
        }

        @Test
        @DisplayName("findByUsername & findByEmail: Trả về đúng Optional User")
        void findByUsernameAndEmail_works() {
            assertThat(userRepository.findByUsername("alice")).isPresent();
            assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
            assertThat(userRepository.findByUsername("nobody")).isEmpty();
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUser(String username, String email, Status status) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("$2a$10$encryptedDummyHashForTesting")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .status(status)
                .failedAttempt(0)
                .build();
        return em.persist(user);
    }

    private Profile createProfile(User user, String fullName, String city, String occupation) {
        Profile profile = Profile.builder()
                .user(user)
                .fullName(fullName)
                .city(city)
                .occupation(occupation)
                .avatarUrl("https://avatar.com/" + user.getUsername() + ".png")
                .dateOfBirth(LocalDate.of(1996, 5, 20))
                .visibility(Visibility.PUBLIC)
                .socialLinks(Map.of("github", "https://github.com/" + user.getUsername()))
                .build();
        return em.persist(profile);
    }

    private void assignRole(User user, Role role) {
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .id(new UserRoleId(user.getId(), role.getId()))
                .build();
        em.persist(userRole);
    }

    private void createBlock(User blocker, User blocked) {
        Block block = Block.builder()
                .id(new BlockId(blocker.getId(), blocked.getId()))
                .blocker(blocker)
                .blocked(blocked)
                .build();
        em.persist(block);
    }

    private Role findOrCreateRole(RoleName roleName, String description) {
        return roleRepository.findAll().stream()
                .filter(r -> r.getName() == roleName)
                .findFirst()
                .orElseGet(() -> em.persist(Role.builder()
                        .name(roleName)
                        .description(description)
                        .build()));
    }
}
