package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.*;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.repository.*;
import net.datafaker.Faker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMockService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final FriendshipRepository friendshipRepository;
    private final BlockRepository blockRepository;
    private final PasswordEncoder passwordEncoder;

    private final Faker faker = new Faker(new Locale("vi"));

    @Transactional
    public void init(Role userRoleName) {

        long count = userRepository.count();

        if (count >= 1000) {
            log.info("Users already initialized");
            return;
        }

        log.info("Starting generating 1000 users with Cloudinary avatars and covers...");

        int batchSize = 100;

        List<User> usersBatch = new ArrayList<>();
        List<Profile> profilesBatch = new ArrayList<>();
        List<UserRole> userRolesBatch = new ArrayList<>();

        String commonPassword = passwordEncoder.encode("123456");

        String[] cities = {"Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ", "Nha Trang", "Đà Lạt", "Huế", "Vũng Tàu"};
        String[] occupations = {"Software Engineer", "Frontend Developer", "Backend Developer", "UI/UX Designer", "Product Manager", "Data Analyst", "Marketing Specialist", "Business Analyst", "DevOps Engineer"};
        String[] companies = {"FPT Software", "VNG Corporation", "Viettel Group", "VNPT", "Shopee Vietnam", "Grab Vietnam", "Techcombank", "Momo"};

        for (int i = 0; i < 1000; i++) {

            String username = faker.internet().username()
                    .replaceAll("[^a-zA-Z0-9._]", "")
                    .toLowerCase()
                    + faker.number().digits(4);

            String email = username + "@" + faker.internet().domainName();

            User user = User.builder()
                    .username(username)
                    .email(email)
                    .emailVerified(true)
                    .passwordHash(commonPassword)
                    .provider(AuthProvider.LOCAL)
                    .status(Status.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 365)))
                    .updatedAt(LocalDateTime.now())
                    .build();

            usersBatch.add(user);

            if (usersBatch.size() == batchSize) {

                userRepository.saveAllAndFlush(usersBatch);

                for (User savedUser : usersBatch) {

                    Gender gender = faker.bool().bool() ? Gender.MALE : Gender.FEMALE;
                    String city = cities[faker.number().numberBetween(0, cities.length)];
                    String occupation = occupations[faker.number().numberBetween(0, occupations.length)];
                    String company = companies[faker.number().numberBetween(0, companies.length)];

                    String avatarUrl = MockDataConstants.getRandomImageUrl();
                    String coverUrl = MockDataConstants.getRandomImageUrl();

                    Map<String, String> socialLinks = Map.of(
                            "facebook", "https://facebook.com/" + savedUser.getUsername(),
                            "github", "https://github.com/" + savedUser.getUsername(),
                            "linkedin", "https://linkedin.com/in/" + savedUser.getUsername()
                    );

                    Profile profile = Profile.builder()
                            .user(savedUser)
                            .fullName(faker.name().fullName())
                            .phone("09" + faker.number().digits(8))
                            .gender(gender)
                            .city(city)
                            .country("Việt Nam")
                            .occupation(occupation)
                            .company(company)
                            .education("Đại học Bách Khoa")
                            .avatarUrl(avatarUrl)
                            .avatarPublicId("")
                            .coverUrl(coverUrl)
                            .coverPublicId("")
                            .dateOfBirth(LocalDate.now().minusYears(faker.number().numberBetween(18, 40)))
                            .bio(MockDataConstants.DATING_BIOS.get(faker.number().numberBetween(0, MockDataConstants.DATING_BIOS.size())))
                            .socialLinks(socialLinks)
                            .visibility(Visibility.PUBLIC)
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build();

                    profilesBatch.add(profile);

                    UserRole uRole = UserRole.builder()
                            .id(new UserRoleId(savedUser.getId(), userRoleName.getId()))
                            .user(savedUser)
                            .role(userRoleName)
                            .assignedAt(LocalDateTime.now())
                            .build();

                    userRolesBatch.add(uRole);
                }

                profileRepository.saveAll(profilesBatch);
                userRoleRepository.saveAll(userRolesBatch);

                usersBatch.clear();
                profilesBatch.clear();
                userRolesBatch.clear();

                log.info("Saved batch of {} users with rich profiles", batchSize);
            }
        }

        log.info("Generating Friendships & Blocks...");

        List<User> allUsers = userRepository.findAll();

        List<Friendship> friendships = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();

        Set<String> uniqueFriendships = new HashSet<>();
        Set<String> uniqueBlocks = new HashSet<>();

        for (User user : allUsers) {

            // Generate friendships
            int friendsCount = faker.number().numberBetween(5, 20);

            for (int i = 0; i < friendsCount; i++) {

                User friend = allUsers.get(faker.number().numberBetween(0, allUsers.size()));

                if (!user.getId().equals(friend.getId())) {

                    Long minId = Math.min(user.getId(), friend.getId());
                    Long maxId = Math.max(user.getId(), friend.getId());

                    String key = minId + "-" + maxId;

                    if (uniqueFriendships.add(key)) {

                        User userOne = user.getId().equals(minId) ? user : friend;
                        User userTwo = user.getId().equals(maxId) ? user : friend;

                        friendships.add(
                                Friendship.builder()
                                        .userOne(userOne)
                                        .userTwo(userTwo)
                                        .requester(user)
                                        .status(media.social.modules.user.enums.FriendshipStatus.ACCEPTED)
                                        .createdAt(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 100)))
                                        .updatedAt(LocalDateTime.now())
                                        .build()
                        );
                    }
                }
            }

            // Generate blocks
            int blocksCount = faker.number().numberBetween(0, 2);

            for (int i = 0; i < blocksCount; i++) {

                User blocked = allUsers.get(faker.number().numberBetween(0, allUsers.size()));

                if (!user.getId().equals(blocked.getId())) {

                    String key = user.getId() + "-" + blocked.getId();

                    if (uniqueBlocks.add(key)) {

                        BlockId blockId = new BlockId();
                        blockId.setBlockerId(user.getId());
                        blockId.setBlockedId(blocked.getId());

                        blocks.add(
                                Block.builder()
                                        .id(blockId)
                                        .blocker(user)
                                        .blocked(blocked)
                                        .createdAt(LocalDateTime.now())
                                        .build()
                        );
                    }
                }
            }
        }

        friendshipRepository.saveAll(friendships);
        blockRepository.saveAll(blocks);

        log.info("User generation complete. Total users: {}, friendships: {}", allUsers.size(), friendships.size());
    }
}