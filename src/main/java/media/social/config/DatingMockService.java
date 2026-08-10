package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.dating.entity.DatingInterest;
import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.entity.DatingPreference;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.entity.DatingProfileInterest;
import media.social.modules.dating.entity.DatingProfileInterestId;
import media.social.modules.dating.entity.DatingSwipe;
import media.social.modules.dating.enums.DatingMatchStatus;
import media.social.modules.dating.enums.DatingSwipeAction;
import media.social.modules.dating.enums.GenderPreference;
import media.social.modules.dating.repository.DatingInterestRepository;
import media.social.modules.dating.repository.DatingMatchRepository;
import media.social.modules.dating.repository.DatingPreferenceRepository;
import media.social.modules.dating.repository.DatingProfileInterestRepository;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.repository.DatingSwipeRepository;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.repository.UserRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatingMockService {

    private final UserRepository userRepository;
    private final DatingProfileRepository datingProfileRepository;
    private final DatingPreferenceRepository datingPreferenceRepository;
    private final DatingSwipeRepository datingSwipeRepository;
    private final DatingMatchRepository datingMatchRepository;
    private final DatingInterestRepository datingInterestRepository;
    private final DatingProfileInterestRepository datingProfileInterestRepository;

    private final Faker faker = new Faker(new Locale("vi"));

    @Transactional
    public void init() {

        // ============================================================
        // 1. Check existing dating profiles
        // ============================================================

        if (datingProfileRepository.count() >= 500) {
            log.info("Dating profiles already initialized");
            return;
        }

        List<User> users = userRepository.findAll();

        if (users.size() < 10) {
            log.warn("Not enough users to generate dating mock data");
            return;
        }

        log.info("Generating Dating Data...");

        // ============================================================
        // 2. Dating interests
        // ============================================================

        String[] interestNames = {
                "Lập trình",
                "Gaming",
                "Du lịch",
                "Gym",
                "Âm nhạc",
                "Phim ảnh",
                "Đọc sách",
                "Nấu ăn",
                "Photography",
                "Cafe",
                "Anime",
                "Bóng đá",
                "Cầu lông",
                "Chạy bộ",
                "Công nghệ"
        };

        List<DatingInterest> interests = new ArrayList<>();

        for (String name : interestNames) {

            DatingInterest interest = datingInterestRepository
                    .findByName(name)
                    .orElseGet(() ->
                            datingInterestRepository.save(
                                    DatingInterest.builder()
                                            .name(name)
                                            .build()
                            )
                    );

            interests.add(interest);
        }

        // ============================================================
        // 3. Generate Dating Profiles
        // ============================================================

        List<DatingProfile> profilesBatch = new ArrayList<>();
        List<DatingPreference> preferencesBatch = new ArrayList<>();
        List<DatingProfileInterest> dpInterestsBatch = new ArrayList<>();

        int batchSize = 100;
        int limit = Math.min(500, users.size());

        for (int i = 0; i < limit; i++) {

            User user = users.get(i);

            // User already has dating profile
            if (datingProfileRepository.existsByUserId(user.getId())) {
                continue;
            }

            Gender gender = faker.bool().bool()
                    ? Gender.MALE
                    : Gender.FEMALE;

            DatingProfile profile = DatingProfile.builder()
                    .user(user)
                    .displayName(faker.name().firstName())
                    .bio(faker.lorem().sentence())
                    .gender(gender)
                    .birthday(
                            LocalDate.now()
                                    .minusYears(
                                            faker.number()
                                                    .numberBetween(18, 40)
                                    )
                    )
                    .height(
                            faker.number()
                                    .numberBetween(150, 190)
                    )
                    .occupation(faker.job().title())
                    .education("University")
                    .latitude(
                            BigDecimal.valueOf(
                                    20.95
                                            + faker.number()
                                            .randomDouble(4, 0, 1)
                            )
                    )
                    .longitude(
                            BigDecimal.valueOf(
                                    105.75
                                            + faker.number()
                                            .randomDouble(4, 0, 1)
                            )
                    )
                    .country("Vietnam")
                    .city("Hà Nội")
                    .active(true)
                    .visibility(Visibility.PUBLIC)
                    .build();

            profilesBatch.add(profile);

            // ========================================================
            // Dating Preference
            // Random MALE / FEMALE
            // ========================================================

            GenderPreference genderPreference =
                    ThreadLocalRandom.current().nextBoolean()
                            ? GenderPreference.MALE
                            : GenderPreference.FEMALE;

            preferencesBatch.add(
                    DatingPreference.builder()
                            .user(user)
                            .minAge(18)
                            .maxAge(40)
                            .genderPreference(genderPreference)
                            .maxDistance(50)
                            .build()
            );

            // ========================================================
            // Save batch
            // ========================================================

            if (profilesBatch.size() == batchSize) {

                saveProfileBatch(
                        profilesBatch,
                        preferencesBatch,
                        dpInterestsBatch,
                        interests
                );

                profilesBatch.clear();
                preferencesBatch.clear();
                dpInterestsBatch.clear();
            }
        }

        // ============================================================
        // 4. Save remaining profiles
        // ============================================================

        if (!profilesBatch.isEmpty()) {

            saveProfileBatch(
                    profilesBatch,
                    preferencesBatch,
                    dpInterestsBatch,
                    interests
            );

            profilesBatch.clear();
            preferencesBatch.clear();
            dpInterestsBatch.clear();
        }

        // ============================================================
        // 5. Generate Swipes & Matches
        // ============================================================

        log.info("Generating Swipes and Matches...");

        List<DatingProfile> allProfiles =
                datingProfileRepository.findAll();

        if (allProfiles.size() < 2) {
            log.warn("Not enough dating profiles to generate swipes");
            return;
        }

        List<DatingSwipe> swipesBatch = new ArrayList<>();
        List<DatingMatch> matchesBatch = new ArrayList<>();

        /*
         * Swipe:
         *
         * swiper_id -> target_id
         *
         * (8, 82) != (82, 8)
         *
         * Match:
         *
         * user_one_id = MIN
         * user_two_id = MAX
         *
         * (8, 82) == (82, 8)
         */

        Set<String> uniqueSwipes = new HashSet<>();
        Set<String> uniqueMatches = new HashSet<>();

        for (DatingProfile p1 : allProfiles) {

            Long swiperId = p1.getUser().getId();

            int swipeCount =
                    faker.number().numberBetween(5, 20);

            for (int j = 0; j < swipeCount; j++) {

                DatingProfile p2 =
                        allProfiles.get(
                                faker.number()
                                        .numberBetween(
                                                0,
                                                allProfiles.size()
                                        )
                        );

                Long targetId = p2.getUser().getId();

                // ====================================================
                // Cannot swipe yourself
                // ====================================================

                if (swiperId.equals(targetId)) {
                    continue;
                }

                // ====================================================
                // Prevent duplicate swipe
                //
                // (8,82) can only appear once
                // ====================================================

                String swipeKey =
                        swiperId + "-" + targetId;

                if (!uniqueSwipes.add(swipeKey)) {
                    continue;
                }

                DatingSwipeAction action =
                        faker.bool().bool()
                                ? DatingSwipeAction.LIKE
                                : DatingSwipeAction.DISLIKE;

                swipesBatch.add(
                        DatingSwipe.builder()
                                .swiper(p1.getUser())
                                .target(p2.getUser())
                                .action(action)
                                .createdAt(OffsetDateTime.now())
                                .build()
                );

                // ====================================================
                // Generate match
                // ====================================================

                if (action == DatingSwipeAction.LIKE
                        && faker.bool().bool()) {

                    Long userId1 = p1.getUser().getId();
                    Long userId2 = p2.getUser().getId();

                    Long minUserId =
                            Math.min(userId1, userId2);

                    Long maxUserId =
                            Math.max(userId1, userId2);

                    String matchKey =
                            minUserId + "-" + maxUserId;

                    /*
                     * Prevent:
                     *
                     * (8,82)
                     * (82,8)
                     *
                     * from creating two matches.
                     */

                    if (uniqueMatches.add(matchKey)) {

                        User userOne =
                                userId1.equals(minUserId)
                                        ? p1.getUser()
                                        : p2.getUser();

                        User userTwo =
                                userId1.equals(minUserId)
                                        ? p2.getUser()
                                        : p1.getUser();

                        matchesBatch.add(
                                DatingMatch.builder()
                                        .userOne(userOne)
                                        .userTwo(userTwo)
                                        .status(
                                                DatingMatchStatus.ACTIVE
                                        )
                                        .matchedAt(
                                                OffsetDateTime.now()
                                        )
                                        .build()
                        );
                    }
                }
            }
        }

        // ============================================================
        // 6. Save Swipes
        // ============================================================

        if (!swipesBatch.isEmpty()) {
            datingSwipeRepository.saveAll(swipesBatch);
        }

        // ============================================================
        // 7. Save Matches
        // ============================================================

        if (!matchesBatch.isEmpty()) {
            datingMatchRepository.saveAll(matchesBatch);
        }

        log.info(
                "Dating generation complete. Profiles={}, Swipes={}, Matches={}",
                allProfiles.size(),
                swipesBatch.size(),
                matchesBatch.size()
        );
    }

    // =================================================================
    // Save profile batch
    // =================================================================

    private void saveProfileBatch(
            List<DatingProfile> profilesBatch,
            List<DatingPreference> preferencesBatch,
            List<DatingProfileInterest> dpInterestsBatch,
            List<DatingInterest> interests
    ) {

        // ============================================================
        // Save profiles first because interests need profile ID
        // ============================================================

        datingProfileRepository.saveAllAndFlush(profilesBatch);

        // ============================================================
        // Save preferences
        // ============================================================

        datingPreferenceRepository.saveAll(preferencesBatch);

        // ============================================================
        // Generate profile interests
        // ============================================================

        for (DatingProfile profile : profilesBatch) {

            int interestCount =
                    faker.number().numberBetween(3, 6);

            Set<Long> usedInterests = new HashSet<>();

            while (usedInterests.size() < interestCount) {

                DatingInterest interest =
                        interests.get(
                                faker.number()
                                        .numberBetween(
                                                0,
                                                interests.size()
                                        )
                        );

                if (!usedInterests.add(interest.getId())) {
                    continue;
                }

                DatingProfileInterestId dpiId =
                        new DatingProfileInterestId(
                                profile.getId(),
                                interest.getId()
                        );

                dpInterestsBatch.add(
                        DatingProfileInterest.builder()
                                .id(dpiId)
                                .datingProfile(profile)
                                .interest(interest)
                                .createdAt(
                                        OffsetDateTime.now()
                                )
                                .build()
                );
            }
        }

        // ============================================================
        // Save profile interests
        // ============================================================

        if (!dpInterestsBatch.isEmpty()) {
            datingProfileInterestRepository.saveAll(
                    dpInterestsBatch
            );
        }
    }
}