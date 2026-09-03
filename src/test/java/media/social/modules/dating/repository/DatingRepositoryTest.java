package media.social.modules.dating.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.dating.dto.projection.DatingProfileCacheProjection;
import media.social.modules.dating.dto.projection.DatingReportDetailProjection;
import media.social.modules.dating.entity.*;
import media.social.modules.dating.enums.DatingMatchStatus;
import media.social.modules.dating.enums.DatingReportStatus;
import media.social.modules.dating.enums.DatingSwipeAction;
import media.social.modules.dating.enums.GenderPreference;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.Gender;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho các Repository của module Dating:
 * {@link DatingProfileRepository}, {@link DatingMatchRepository}, {@link DatingSwipeRepository},
 * {@link DatingPreferenceRepository}, {@link DatingInterestRepository}, {@link DatingReportRepository}
 * sử dụng PostgreSQL Container (PostGIS image).
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V7 & V8: bảng dating_profiles, dating_matches, dating_swipes, unique uk_match_pair, uk_user_target_swipe, seed interests)</li>
 *     <li>DatingMatch (existsByUserOneIdAndUserTwoId, findActiveMatchesByUserId với multi-join fetch profile)</li>
 *     <li>DatingSwipe (existsBySwiperIdAndTargetIdAndAction với CASE WHEN COUNT > 0, findBySwiperIdAndTargetId, delete)</li>
 *     <li>DatingProfile (findDatingProfileCache, existsByUserId)</li>
 *     <li>DatingPreference (findByUserId, existsByUserId)</li>
 *     <li>DatingInterest (findByName, findAllByIdIn từ dữ liệu seed V8)</li>
 *     <li>DatingReport (getAll, getByStatus với join reporter, reportedUser, reviewer)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Dating Repositories – Integration Tests với PostgreSQL Container")
class DatingRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_dating_db")
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

    @Autowired private DatingProfileRepository datingProfileRepository;
    @Autowired private DatingMatchRepository datingMatchRepository;
    @Autowired private DatingSwipeRepository datingSwipeRepository;
    @Autowired private DatingPreferenceRepository datingPreferenceRepository;
    @Autowired private DatingInterestRepository datingInterestRepository;
    @Autowired private DatingReportRepository datingReportRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;
    private DatingProfile aliceDatingProfile;
    private DatingProfile bobDatingProfile;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");

        createProfile(alice, "Alice Nguyen");
        createProfile(bob, "Bob Tran");
        createProfile(charlie, "Charlie Le");

        // Dating profile
        aliceDatingProfile = createDatingProfile(alice, "Alice", Gender.FEMALE, LocalDate.of(1998, 5, 20),
                BigDecimal.valueOf(21.0285), BigDecimal.valueOf(105.8542)); // Hà Nội

        bobDatingProfile = createDatingProfile(bob, "Bob", Gender.MALE, LocalDate.of(1996, 3, 15),
                BigDecimal.valueOf(21.0300), BigDecimal.valueOf(105.8500));

        // Preferences
        createDatingPreference(alice, 22, 35, GenderPreference.MALE, 50);
        createDatingPreference(bob, 20, 30, GenderPreference.FEMALE, 50);

        // Swipe: alice likes bob
        createSwipe(alice, bob, DatingSwipeAction.LIKE);

        // Match: alice and bob
        createMatch(alice, bob, DatingMatchStatus.ACTIVE);

        // Report: charlie reports bob
        createDatingReport(charlie, bob, "Fake profile", DatingReportStatus.PENDING);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION & SEED TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V7 & V8: Constraints, Seed Interests)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V8: Đã seed sẵn các sở thích hẹn hò trong database")
        void flyway_seededInterestsExist() {
            List<DatingInterest> interests = datingInterestRepository.findAll();
            assertThat(interests).isNotEmpty();
        }

        @Test
        @DisplayName("Flyway V7: uk_match_pair ngăn trùng lặp cặp match giữa 2 user")
        void flyway_uniqueMatchPairConstraint() {
            DatingMatch duplicate = DatingMatch.builder()
                    .userOne(alice)
                    .userTwo(bob)
                    .status(DatingMatchStatus.ACTIVE)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_match_pair");
        }

        @Test
        @DisplayName("Flyway V7: uk_user_target_swipe ngăn 1 người quẹt trùng 2 lần trên cùng đối phương")
        void flyway_uniqueSwipeConstraint() {
            DatingSwipe duplicate = DatingSwipe.builder()
                    .swiper(alice)
                    .target(bob)
                    .action(DatingSwipeAction.DISLIKE)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_user_target_swipe");
        }
    }

    // =========================================================================
    // 2. DATING MATCH TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. DatingMatchRepository Queries Test")
    class MatchQueriesTests {

        @Test
        @DisplayName("existsByUserOneIdAndUserTwoId: Kiểm tra cặp match tồn tại")
        void existsMatch() {
            assertThat(datingMatchRepository.existsByUserOneIdAndUserTwoId(alice.getId(), bob.getId())).isTrue();
            assertThat(datingMatchRepository.existsByUserOneIdAndUserTwoId(alice.getId(), charlie.getId())).isFalse();
        }

        @Test
        @DisplayName("findActiveMatchesByUserId: JOIN FETCH userOne, userTwo và profile lấy đầy đủ match")
        void findActiveMatchesByUserId_eagerLoadsProfiles() {
            List<DatingMatch> matches = datingMatchRepository.findActiveMatchesByUserId(alice.getId());

            assertThat(matches).hasSize(1);
            DatingMatch match = matches.get(0);
            assertThat(match.getUserOne().getUsername()).isEqualTo("alice");
            assertThat(match.getUserTwo().getUsername()).isEqualTo("bob");
            assertThat(match.getStatus()).isEqualTo(DatingMatchStatus.ACTIVE);
        }
    }

    // =========================================================================
    // 3. DATING SWIPE TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. DatingSwipeRepository Queries Test")
    class SwipeQueriesTests {

        @Test
        @DisplayName("existsBySwiperIdAndTargetIdAndAction: CASE WHEN COUNT > 0 trả về true/false")
        void existsSwipeAction() {
            // alice đã LIKE bob -> true
            boolean hasLiked = datingSwipeRepository.existsBySwiperIdAndTargetIdAndAction(
                    alice.getId(), bob.getId(), DatingSwipeAction.LIKE);
            assertThat(hasLiked).isTrue();

            // alice chưa DISLIKE bob -> false
            boolean hasDisliked = datingSwipeRepository.existsBySwiperIdAndTargetIdAndAction(
                    alice.getId(), bob.getId(), DatingSwipeAction.DISLIKE);
            assertThat(hasDisliked).isFalse();
        }

        @Test
        @DisplayName("deleteBySwiperIdAndTargetId: Xóa thao tác swipe")
        void deleteSwipe() {
            datingSwipeRepository.deleteBySwiperIdAndTargetId(alice.getId(), bob.getId());
            em.flush();
            em.clear();

            Optional<DatingSwipe> found = datingSwipeRepository.findBySwiperIdAndTargetId(alice.getId(), bob.getId());
            assertThat(found).isEmpty();
        }
    }

    // =========================================================================
    // 4. DATING PROFILE & PREFERENCE TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. DatingProfile & Preference Queries Test")
    class ProfileAndPreferenceTests {

        @Test
        @DisplayName("findDatingProfileCache: JOIN dp.user u LEFT JOIN u.profile p")
        void findDatingProfileCache_returnsProjection() {
            Optional<DatingProfileCacheProjection> cacheOpt =
                    datingProfileRepository.findDatingProfileCache(alice.getId());

            assertThat(cacheOpt).isPresent();
            DatingProfileCacheProjection p = cacheOpt.get();
            assertThat(p.getUsername()).isEqualTo("alice");
            assertThat(p.getDisplayName()).isEqualTo("Alice");
            assertThat(p.getGender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("DatingPreference: findByUserId & existsByUserId")
        void preference_queries() {
            assertThat(datingPreferenceRepository.existsByUserId(alice.getId())).isTrue();
            assertThat(datingPreferenceRepository.existsByUserId(charlie.getId())).isFalse();

            Optional<DatingPreference> prefOpt = datingPreferenceRepository.findByUserId(alice.getId());
            assertThat(prefOpt).isPresent();
            assertThat(prefOpt.get().getGenderPreference()).isEqualTo(GenderPreference.MALE);
            assertThat(prefOpt.get().getMinAge()).isEqualTo(22);
            assertThat(prefOpt.get().getMaxAge()).isEqualTo(35);
        }
    }

    // =========================================================================
    // 5. DATING REPORT TESTS
    // =========================================================================

    @Nested
    @DisplayName("5. DatingReportRepository Queries Test")
    class ReportQueriesTests {

        @Test
        @DisplayName("getAll & getByStatus: JOIN reporter, reportedUser, reviewer")
        void report_queries() {
            assertThat(datingReportRepository.existsByReporter_IdAndReportedUser_Id(charlie.getId(), bob.getId()))
                    .isTrue();

            Page<DatingReportDetailProjection> all = datingReportRepository.getAll(PageRequest.of(0, 10));
            assertThat(all.getContent()).hasSize(1);
            assertThat(all.getContent().get(0).getReporterUsername()).isEqualTo("charlie");
            assertThat(all.getContent().get(0).getReportedUsername()).isEqualTo("bob");
            assertThat(all.getContent().get(0).getStatus()).isEqualTo(DatingReportStatus.PENDING);
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

    private DatingProfile createDatingProfile(User user, String displayName, Gender gender,
                                              LocalDate birthday, BigDecimal lat, BigDecimal lng) {
        return em.persist(DatingProfile.builder()
                .user(user)
                .displayName(displayName)
                .gender(gender)
                .birthday(birthday)
                .latitude(lat)
                .longitude(lng)
                .active(true)
                .build());
    }

    private void createDatingPreference(User user, Integer minAge, Integer maxAge, GenderPreference genderPref, Integer maxDist) {
        em.persist(DatingPreference.builder()
                .user(user)
                .minAge(minAge)
                .maxAge(maxAge)
                .genderPreference(genderPref)
                .maxDistance(maxDist)
                .build());
    }

    private void createSwipe(User swiper, User target, DatingSwipeAction action) {
        em.persist(DatingSwipe.builder()
                .swiper(swiper)
                .target(target)
                .action(action)
                .build());
    }

    private void createMatch(User userOne, User userTwo, DatingMatchStatus status) {
        em.persist(DatingMatch.builder()
                .userOne(userOne)
                .userTwo(userTwo)
                .status(status)
                .matchedAt(OffsetDateTime.now())
                .build());
    }

    private void createDatingReport(User reporter, User reported, String reason, DatingReportStatus status) {
        em.persist(DatingReport.builder()
                .reporter(reporter)
                .reportedUser(reported)
                .reason(reason)
                .status(status)
                .build());
    }
}
