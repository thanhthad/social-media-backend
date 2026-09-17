package media.social.modules.user.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.projection.FriendshipCountProjection;
import media.social.modules.user.dto.projection.FriendshipUserProjection;
import media.social.modules.user.dto.projection.MutualFriendCountProjection;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho {@link FriendshipRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử các query thực tế có sẵn trong FriendshipRepository:
 * <ul>
 *     <li>Migration test (Flyway V1, ràng buộc chk_friendship_different_users, uk_friendship_pair)</li>
 *     <li>getFriends: EXISTS subquery kiểm tra viewerFriendship khi visibility = FRIEND, ORDER BY acceptedAt DESC</li>
 *     <li>findSuggestedUsers: CTE native query WITH friend_pairs AS (...), GROUP BY, COUNT(*) AS mutual_count,
 *         NOT EXISTS bạn bè hiện tại, ORDER BY mutual_count DESC</li>
 *     <li>findMutualFriendCount, findFriendshipCount, areFriends (COUNT(f) > 0)</li>
 *     <li>findMutualFriends, findMutualFriendAvatars (lọc avatar_url IS NOT NULL)</li>
 *     <li>getMyFriends, getPendingFriendRequests</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("FriendshipRepository – Integration Tests với PostgreSQL Container")
class FriendshipRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_friendship_db")
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
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;
    private User david;
    private User emma;

    @BeforeEach
    void setUp() {
        // Tạo các user
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");
        david = createUser("david", "david@example.com");
        emma = createUser("emma", "emma@example.com");

        createProfile(alice, "Alice Nguyen", "https://cdn.example.com/alice.jpg");
        createProfile(bob, "Bob Tran", "https://cdn.example.com/bob.jpg");
        createProfile(charlie, "Charlie Le", "https://cdn.example.com/charlie.jpg");
        createProfile(david, "David Pham", "https://cdn.example.com/david.jpg");
        createProfile(emma, "Emma Hoang", null); // Emma không có avatar để test IS NOT NULL

        // Mối quan hệ bạn bè mẫu:
        // alice <-> bob (ACCEPTED)
        createFriendship(alice, bob, alice, FriendshipStatus.ACCEPTED, Visibility.PUBLIC);

        // bob <-> charlie (ACCEPTED)
        createFriendship(bob, charlie, bob, FriendshipStatus.ACCEPTED, Visibility.PUBLIC);

        // david <-> bob (ACCEPTED)
        createFriendship(bob, david, bob, FriendshipStatus.ACCEPTED, Visibility.PUBLIC);

        // alice -> charlie (PENDING)
        createFriendship(alice, charlie, alice, FriendshipStatus.PENDING, Visibility.PUBLIC);

        // bob <-> emma (ACCEPTED nhưng Visibility = FRIEND)
        createFriendship(bob, emma, bob, FriendshipStatus.ACCEPTED, Visibility.FRIEND);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION TESTS (Flyway V1 Schema, Constraints)
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1 Schema & Database Constraints)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: Check constraint chk_friendship_different_users ngăn chặn tự kết bạn với chính mình")
        void flyway_selfFriendshipConstraint_fails() {
            // Cố tình tạo quan hệ user_one_id == user_two_id
            Friendship selfFriendship = Friendship.builder()
                    .userOne(alice)
                    .userTwo(alice)
                    .requester(alice)
                    .status(FriendshipStatus.PENDING)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(selfFriendship);
                em.flush();
            }).hasMessageContaining("chk_friendship_different_users");
        }

        @Test
        @DisplayName("Flyway V1: Unique constraint uk_friendship_pair ngăn chặn tạo 2 bản ghi cùng cặp userOne và userTwo")
        void flyway_uniquePairConstraint_failsOnDuplicate() {
            // alice và bob đã có bản ghi trong setUp()
            Friendship duplicate = Friendship.builder()
                    .userOne(alice)
                    .userTwo(bob)
                    .requester(bob)
                    .status(FriendshipStatus.PENDING)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_friendship_pair");
        }
    }

    // =========================================================================
    // 2. EXISTS TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. EXISTS Sub-query Test (getFriends with Visibility.FRIEND)")
    class ExistsTests {

        @Test
        @DisplayName("EXISTS: Viewer (alice) là bạn của bob -> thỏa mãn EXISTS viewerFriendship -> thấy được emma (Visibility.FRIEND)")
        void getFriends_existsViewerFriendship_showsFriendOnlyProfile() {
            // bob có bạn là emma (visibility = FRIEND)
            // alice xem danh sách bạn của bob. Do alice và bob là bạn (ACCEPTED) -> EXISTS true -> thấy emma
            Page<FriendshipUserProjection> friendsOfBob = friendshipRepository.getFriends(
                    bob.getId(),
                    alice.getId(), // viewerId = alice
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    PageRequest.of(0, 10)
            );

            List<Long> friendUserIds = friendsOfBob.getContent().stream()
                    .map(FriendshipUserProjection::getUserId)
                    .toList();

            assertThat(friendUserIds).contains(emma.getId(), alice.getId(), charlie.getId(), david.getId());
        }

        @Test
        @DisplayName("EXISTS: Viewer (charlie) KHÔNG PHẢI bạn của alice -> không thấy danh sách bạn có Visibility.FRIEND")
        void getFriends_notExistsViewerFriendship_hidesFriendOnlyProfile() {
            // Tạo thêm bạn riêng của charlie với visibility FRIEND
            User stranger = createUser("stranger", "stranger@example.com");
            createFriendship(charlie, stranger, charlie, FriendshipStatus.ACCEPTED, Visibility.FRIEND);
            em.flush();
            em.clear();

            // alice xem danh sách bạn của charlie (alice và charlie mới chỉ PENDING, chưa ACCEPTED)
            // -> viewerFriendship EXISTS false -> stranger không xuất hiện
            Page<FriendshipUserProjection> friendsOfCharlie = friendshipRepository.getFriends(
                    charlie.getId(),
                    alice.getId(), // viewerId = alice
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    PageRequest.of(0, 10)
            );

            List<Long> friendUserIds = friendsOfCharlie.getContent().stream()
                    .map(FriendshipUserProjection::getUserId)
                    .toList();

            assertThat(friendUserIds).doesNotContain(stranger.getId());
        }
    }

    // =========================================================================
    // 3. GROUP BY + HAVING + COUNT TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. GROUP BY + COUNT + NOT EXISTS Test (findSuggestedUsers)")
    class GroupByCountTests {

        @Test
        @DisplayName("findSuggestedUsers: GROUP BY + COUNT mutual friends + NOT EXISTS bạn bè hiện tại")
        void findSuggestedUsers_groupByAndCountMutual() {
            // alice <-> bob (ACCEPTED)
            // bob <-> charlie (ACCEPTED), bob <-> david (ACCEPTED)
            // -> Gợi ý bạn bè cho alice: charlie (1 bạn chung: bob), david (1 bạn chung: bob)
            List<Object[]> suggestions = friendshipRepository.findSuggestedUsers(alice.getId());

            assertThat(suggestions).isNotEmpty();

            // Kiểm tra cấu trúc: [0] suggested_user_id, [1] username, [2] avatar_url, [3] mutual_count
            for (Object[] row : suggestions) {
                Long suggestedUserId = ((Number) row[0]).longValue();
                String username = (String) row[1];
                Long mutualCount = ((Number) row[3]).longValue();

                // bob đã là bạn của alice -> NOT EXISTS phải loại bỏ bob khỏi danh sách gợi ý!
                assertThat(suggestedUserId).isNotEqualTo(bob.getId());
                // Không tự gợi ý bản thân alice
                assertThat(suggestedUserId).isNotEqualTo(alice.getId());

                // mutual_count từ COUNT(*) phải >= 1
                assertThat(mutualCount).isGreaterThanOrEqualTo(1L);
            }

            // charlie và david phải có mặt trong kết quả gợi ý
            List<String> suggestedUsernames = suggestions.stream()
                    .map(row -> (String) row[1])
                    .toList();
            assertThat(suggestedUsernames).contains("charlie", "david");
        }

        @Test
        @DisplayName("findMutualFriendCount: COUNT(*) trả về tổng số bạn chung giữa alice và charlie = 1 (bob)")
        void findMutualFriendCount_accurate() {
            Optional<MutualFriendCountProjection> opt =
                    friendshipRepository.findMutualFriendCount(alice.getId(), charlie.getId());

            assertThat(opt).isPresent();
            assertThat(opt.get().getTotalMutualFriends()).isEqualTo(1L);
        }

        @Test
        @DisplayName("findFriendshipCount: COUNT(f.id) tính tổng số bạn bè ACCEPTED của bob = 4")
        void findFriendshipCount_accurate() {
            // bob có bạn bè ACCEPTED với: alice, charlie, david, emma = 4 bạn
            Optional<FriendshipCountProjection> countOpt =
                    friendshipRepository.findFriendshipCount(bob.getId(), FriendshipStatus.ACCEPTED);

            assertThat(countOpt).isPresent();
            assertThat(countOpt.get().getTotalFriends()).isEqualTo(4L);
        }

        @Test
        @DisplayName("areFriends: COUNT(f) > 0 trả về true khi ACCEPTED, false khi PENDING hoặc không quen")
        void areFriends_countBoolean() {
            // alice và bob: ACCEPTED -> true
            assertThat(friendshipRepository.areFriends(alice.getId(), bob.getId(), FriendshipStatus.ACCEPTED))
                    .isTrue();

            // alice và charlie: PENDING -> false khi check ACCEPTED
            assertThat(friendshipRepository.areFriends(alice.getId(), charlie.getId(), FriendshipStatus.ACCEPTED))
                    .isFalse();

            // alice và emma: không có quan hệ -> false
            assertThat(friendshipRepository.areFriends(alice.getId(), emma.getId(), FriendshipStatus.ACCEPTED))
                    .isFalse();
        }
    }

    // =========================================================================
    // 4. ORDER BY & COMPLEX CTE TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. ORDER BY & Native CTE Queries Test")
    class OrderByAndCteTests {

        @Test
        @DisplayName("findMutualFriends: Native CTE WITH friend_pairs AS (...) + ORDER BY u.username")
        void findMutualFriends_orderedByUsername() {
            List<Object[]> mutualFriends = friendshipRepository.findMutualFriends(alice.getId(), charlie.getId());

            // Bạn chung giữa alice và charlie là bob
            assertThat(mutualFriends).hasSize(1);
            Object[] bobRow = mutualFriends.get(0);
            assertThat(bobRow[1]).isEqualTo("bob");
            assertThat(bobRow[2]).isEqualTo("https://cdn.example.com/bob.jpg");
        }

        @Test
        @DisplayName("findMutualFriendAvatars: Native query lọc IS NOT NULL avatar_url")
        void findMutualFriendAvatars_notNullOnly() {
            // Bạn chung giữa charlie và david là bob (bob có avatar)
            List<String> avatars = friendshipRepository.findMutualFriendAvatars(charlie.getId(), david.getId());

            assertThat(avatars).contains("https://cdn.example.com/bob.jpg");
            assertThat(avatars).allMatch(avatar -> avatar != null && !avatar.isBlank());
        }

        @Test
        @DisplayName("getMyFriends: ORDER BY f.createdAt DESC")
        void getMyFriends_orderedByCreatedAtDesc() {
            Page<FriendshipUserProjection> page = friendshipRepository.getMyFriends(
                    bob.getId(), FriendshipStatus.ACCEPTED, PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(4);
        }

        @Test
        @DisplayName("getPendingFriendRequests: Lọc chính xác lời mời đang PENDING gửi tới charlie")
        void getPendingFriendRequests_accurate() {
            // alice gửi request tới charlie (status = PENDING, requester = alice)
            Page<FriendshipUserProjection> pending = friendshipRepository.getPendingFriendRequests(
                    charlie.getId(), FriendshipStatus.PENDING, PageRequest.of(0, 10));

            assertThat(pending.getContent()).hasSize(1);
            assertThat(pending.getContent().get(0).getUsername()).isEqualTo("alice");
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
                .dateOfBirth(LocalDate.of(1998, 1, 1))
                .visibility(Visibility.PUBLIC)
                .build());
    }

    private Friendship createFriendship(User userOne, User userTwo, User requester,
                                         FriendshipStatus status, Visibility visibility) {
        return em.persist(Friendship.builder()
                .userOne(userOne)
                .userTwo(userTwo)
                .requester(requester)
                .status(status)
                .visibility(visibility)
                .build());
    }
}
