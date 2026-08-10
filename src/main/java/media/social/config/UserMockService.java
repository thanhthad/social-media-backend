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
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final FollowRepository followRepository;
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

        log.info("Starting generating 1000 users...");
        int batchSize = 100;
        List<User> usersBatch = new ArrayList<>();
        List<Profile> profilesBatch = new ArrayList<>();
        List<UserRole> userRolesBatch = new ArrayList<>();
        
        String commonPassword = passwordEncoder.encode("123456");

        for (int i = 0; i < 1000; i++) {
            String username = faker.name().username() + faker.number().digits(4);
            String email = username + "@" + faker.internet().domainName();

            User user = User.builder()
                    .username(username)
                    .email(email)
                    .emailVerified(true)
                    .passwordHash(commonPassword)
                    .provider(AuthProvider.LOCAL)
                    .status(Status.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            usersBatch.add(user);

            if (usersBatch.size() == batchSize) {
                userRepository.saveAllAndFlush(usersBatch);
                for (User savedUser : usersBatch) {
                    Profile profile = Profile.builder()
                            .user(savedUser)
                            .fullName(faker.name().fullName())
                            .phone(faker.phoneNumber().cellPhone())
                            .gender(faker.bool().bool() ? Gender.MALE : Gender.FEMALE)
                            .city(faker.address().city())
                            .dateOfBirth(LocalDate.now().minusYears(18 + faker.number().numberBetween(0, 30)))
                            .bio(faker.lorem().sentence())
                            .socialLinks(Map.of())
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
                log.info("Saved batch of {} users", batchSize);
            }
        }
        
        log.info("Generating Follows & Blocks...");
        List<User> allUsers = userRepository.findAll();
        List<Follow> follows = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        Set<String> uniqueFollows = new HashSet<>();
        Set<String> uniqueBlocks = new HashSet<>();

        for (User user : allUsers) {
            int followsCount = faker.number().numberBetween(5, 15);
            for (int i = 0; i < followsCount; i++) {
                User following = allUsers.get(faker.number().numberBetween(0, allUsers.size()));
                if (!user.getId().equals(following.getId())) {
                    String key = user.getId() + "-" + following.getId();
                    if (uniqueFollows.add(key)) {
                        FollowId followId = new FollowId(user.getId(), following.getId());
                        follows.add(Follow.builder()
                                .id(followId)
                                .follower(user)
                                .following(following)
                                .createdAt(LocalDateTime.now())
                                .build());
                    }
                }
            }

            int blocksCount = faker.number().numberBetween(0, 2);
            for (int i = 0; i < blocksCount; i++) {
                User blocked = allUsers.get(faker.number().numberBetween(0, allUsers.size()));
                if (!user.getId().equals(blocked.getId())) {
                    String key = user.getId() + "-" + blocked.getId();
                    if (uniqueBlocks.add(key)) {
                        BlockId blockId = new BlockId();
                        blockId.setBlockerId(user.getId());
                        blockId.setBlockedId(blocked.getId());
                        blocks.add(Block.builder()
                                .id(blockId)
                                .blocker(user)
                                .blocked(blocked)
                                .createdAt(LocalDateTime.now())
                                .build());
                    }
                }
            }
        }
        followRepository.saveAll(follows);
        blockRepository.saveAll(blocks);
        log.info("User generation complete");
    }
}
