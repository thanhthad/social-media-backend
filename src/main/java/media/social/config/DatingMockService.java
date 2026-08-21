package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.dating.entity.*;
import media.social.modules.dating.enums.DatingMatchStatus;
import media.social.modules.dating.enums.DatingReportStatus;
import media.social.modules.dating.enums.DatingSwipeAction;
import media.social.modules.dating.enums.GenderPreference;
import media.social.modules.dating.repository.*;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.repository.UserRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatingMockService {

    private final UserRepository userRepository;
    private final DatingProfileRepository datingProfileRepository;
    private final DatingProfilePhotoRepository datingProfilePhotoRepository;
    private final DatingPreferenceRepository datingPreferenceRepository;
    private final DatingSwipeRepository datingSwipeRepository;
    private final DatingMatchRepository datingMatchRepository;
    private final DatingInterestRepository datingInterestRepository;
    private final DatingProfileInterestRepository datingProfileInterestRepository;
    private final DatingReportRepository datingReportRepository;

    private final Faker faker = new Faker(new Locale("vi"));

    private record CityLocation(String name, double latitude, double longitude) {}

    private static final List<CityLocation> VIETNAM_CITIES = List.of(
            new CityLocation("Hà Nội", 21.0285, 105.8542),
            new CityLocation("Hải Phòng", 20.8449, 106.6881),
            new CityLocation("Đà Nẵng", 16.0544, 108.2022),
            new CityLocation("Huế", 16.4637, 107.5909),
            new CityLocation("Nha Trang", 12.2388, 109.1967),
            new CityLocation("Đà Lạt", 11.9404, 108.4583),
            new CityLocation("TP.HCM", 10.8231, 106.6297),
            new CityLocation("Cần Thơ", 10.0452, 105.7469),
            new CityLocation("Vũng Tàu", 10.4114, 107.1362),
            new CityLocation("Biên Hòa", 10.9574, 106.842)
    );

    @Transactional
    public void init() {

        if (datingProfileRepository.count() >= 500) {
            log.info("Dating profiles already initialized");
            return;
        }

        List<User> users = userRepository.findAll();

        if (users.size() < 10) {
            log.warn("Not enough users to generate dating mock data");
            return;
        }

        log.info("Generating Dating Profiles with Cloudinary Photos, Preferences, and Interests...");

        String[] interestNames = {
                "Lập trình", "Gaming", "Du lịch", "Gym", "Âm nhạc", "Phim ảnh", "Đọc sách", "Nấu ăn",
                "Photography", "Cafe", "Anime", "Bóng đá", "Cầu lông", "Chạy bộ", "Công nghệ", "Thú cưng", "Yoga"
        };

        List<DatingInterest> interests = new ArrayList<>();
        for (String name : interestNames) {
            DatingInterest interest = datingInterestRepository
                    .findByName(name)
                    .orElseGet(() -> datingInterestRepository.save(DatingInterest.builder().name(name).build()));
            interests.add(interest);
        }

        List<DatingProfile> profilesBatch = new ArrayList<>();
        List<DatingPreference> preferencesBatch = new ArrayList<>();
        List<DatingProfileInterest> dpInterestsBatch = new ArrayList<>();
        List<DatingProfilePhoto> dpPhotosBatch = new ArrayList<>();

        int batchSize = 100;
        int limit = Math.min(500, users.size());

        for (int i = 0; i < limit; i++) {
            User user = users.get(i);

            if (datingProfileRepository.existsByUserId(user.getId())) {
                continue;
            }

            Gender gender = faker.bool().bool() ? Gender.MALE : Gender.FEMALE;
            CityLocation city = VIETNAM_CITIES.get(faker.number().numberBetween(0, VIETNAM_CITIES.size()));

            double latOffset = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
            double lngOffset = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);

            double finalLatitude = city.latitude() + latOffset;
            double finalLongitude = city.longitude() + lngOffset;

            String bio = MockDataConstants.DATING_BIOS.get(
                    faker.number().numberBetween(0, MockDataConstants.DATING_BIOS.size())
            );

            DatingProfile profile = DatingProfile.builder()
                    .user(user)
                    .displayName(faker.name().firstName())
                    .bio(bio)
                    .gender(gender)
                    .birthday(LocalDate.now().minusYears(faker.number().numberBetween(18, 38)))
                    .height(faker.number().numberBetween(155, 188))
                    .occupation(faker.job().title())
                    .education("Đại học")
                    .latitude(BigDecimal.valueOf(finalLatitude))
                    .longitude(BigDecimal.valueOf(finalLongitude))
                    .country("Việt Nam")
                    .city(city.name())
                    .active(true)
                    .visibility(Visibility.PUBLIC)
                    .build();

            profilesBatch.add(profile);

            GenderPreference genderPreference = ThreadLocalRandom.current().nextBoolean()
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

            if (profilesBatch.size() == batchSize) {
                saveProfileBatch(
                        profilesBatch,
                        preferencesBatch,
                        dpInterestsBatch,
                        dpPhotosBatch,
                        interests
                );

                profilesBatch.clear();
                preferencesBatch.clear();
                dpInterestsBatch.clear();
                dpPhotosBatch.clear();
            }
        }

        if (!profilesBatch.isEmpty()) {
            saveProfileBatch(
                    profilesBatch,
                    preferencesBatch,
                    dpInterestsBatch,
                    dpPhotosBatch,
                    interests
            );

            profilesBatch.clear();
            preferencesBatch.clear();
            dpInterestsBatch.clear();
            dpPhotosBatch.clear();
        }

        log.info("Generating Dating Swipes, Matches, and Reports...");

        List<DatingProfile> allProfiles = datingProfileRepository.findAll();
        if (allProfiles.size() < 2) return;

        List<DatingSwipe> swipesBatch = new ArrayList<>();
        List<DatingMatch> matchesBatch = new ArrayList<>();
        List<DatingReport> reportsBatch = new ArrayList<>();

        Set<String> uniqueSwipes = new HashSet<>();
        Set<String> uniqueMatches = new HashSet<>();

        for (DatingProfile p1 : allProfiles) {
            Long swiperId = p1.getUser().getId();
            int swipeCount = faker.number().numberBetween(5, 20);

            for (int j = 0; j < swipeCount; j++) {
                DatingProfile p2 = allProfiles.get(faker.number().numberBetween(0, allProfiles.size()));
                Long targetId = p2.getUser().getId();

                if (swiperId.equals(targetId)) continue;

                String swipeKey = swiperId + "-" + targetId;
                if (!uniqueSwipes.add(swipeKey)) continue;

                DatingSwipeAction action = faker.bool().bool() ? DatingSwipeAction.LIKE : DatingSwipeAction.DISLIKE;

                swipesBatch.add(
                        DatingSwipe.builder()
                                .swiper(p1.getUser())
                                .target(p2.getUser())
                                .action(action)
                                .createdAt(OffsetDateTime.now().minusDays(faker.number().numberBetween(0, 30)))
                                .build()
                );

                if (action == DatingSwipeAction.LIKE && faker.bool().bool()) {
                    Long userId1 = p1.getUser().getId();
                    Long userId2 = p2.getUser().getId();

                    Long minUserId = Math.min(userId1, userId2);
                    Long maxUserId = Math.max(userId1, userId2);

                    String matchKey = minUserId + "-" + maxUserId;

                    if (uniqueMatches.add(matchKey)) {
                        User userOne = userId1.equals(minUserId) ? p1.getUser() : p2.getUser();
                        User userTwo = userId1.equals(minUserId) ? p2.getUser() : p1.getUser();

                        matchesBatch.add(
                                DatingMatch.builder()
                                        .userOne(userOne)
                                        .userTwo(userTwo)
                                        .status(DatingMatchStatus.ACTIVE)
                                        .matchedAt(OffsetDateTime.now().minusDays(faker.number().numberBetween(0, 20)))
                                        .build()
                        );
                    }
                }
            }

            // A few dating reports
            if (faker.number().numberBetween(1, 100) > 96) {
                DatingProfile reported = allProfiles.get(faker.number().numberBetween(0, allProfiles.size()));
                if (!p1.getUser().getId().equals(reported.getUser().getId())) {
                    String reason = MockDataConstants.REPORT_REASONS.get(
                            faker.number().numberBetween(0, MockDataConstants.REPORT_REASONS.size())
                    );
                    reportsBatch.add(DatingReport.builder()
                            .reporter(p1.getUser())
                            .reportedUser(reported.getUser())
                            .reason(reason)
                            .status(DatingReportStatus.PENDING)
                            .build());
                }
            }
        }

        if (!swipesBatch.isEmpty()) {
            datingSwipeRepository.saveAll(swipesBatch);
        }
        if (!matchesBatch.isEmpty()) {
            datingMatchRepository.saveAll(matchesBatch);
        }
        if (!reportsBatch.isEmpty()) {
            datingReportRepository.saveAll(reportsBatch);
        }

        log.info(
                "Dating generation complete. Profiles={}, Swipes={}, Matches={}, Reports={}",
                allProfiles.size(),
                swipesBatch.size(),
                matchesBatch.size(),
                reportsBatch.size()
        );
    }

    private void saveProfileBatch(
            List<DatingProfile> profilesBatch,
            List<DatingPreference> preferencesBatch,
            List<DatingProfileInterest> dpInterestsBatch,
            List<DatingProfilePhoto> dpPhotosBatch,
            List<DatingInterest> interests
    ) {
        datingProfileRepository.saveAllAndFlush(profilesBatch);
        datingPreferenceRepository.saveAll(preferencesBatch);

        for (DatingProfile profile : profilesBatch) {
            // Photos (2 to 5 photos per profile from Cloudinary)
            int photoCount = faker.number().numberBetween(2, 5);
            for (int pi = 0; pi < photoCount; pi++) {
                DatingProfilePhoto photo = DatingProfilePhoto.builder()
                        .datingProfile(profile)
                        .imageUrl(MockDataConstants.getRandomImageUrl())
                        .publicId("")
                        .displayOrder(pi)
                        .primary(pi == 0)
                        .createdAt(LocalDateTime.now())
                        .build();
                dpPhotosBatch.add(photo);
            }

            // Interests
            int interestCount = faker.number().numberBetween(3, 6);
            Set<Long> usedInterests = new HashSet<>();

            while (usedInterests.size() < interestCount) {
                DatingInterest interest = interests.get(
                        faker.number().numberBetween(0, interests.size())
                );

                if (!usedInterests.add(interest.getId())) {
                    continue;
                }

                DatingProfileInterestId dpiId = new DatingProfileInterestId(profile.getId(), interest.getId());

                dpInterestsBatch.add(
                        DatingProfileInterest.builder()
                                .id(dpiId)
                                .datingProfile(profile)
                                .interest(interest)
                                .createdAt(OffsetDateTime.now())
                                .build()
                );
            }
        }

        if (!dpPhotosBatch.isEmpty()) {
            datingProfilePhotoRepository.saveAll(dpPhotosBatch);
        }
        if (!dpInterestsBatch.isEmpty()) {
            datingProfileInterestRepository.saveAll(dpInterestsBatch);
        }
    }
}