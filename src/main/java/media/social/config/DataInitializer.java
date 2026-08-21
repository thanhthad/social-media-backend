package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.Role;
import media.social.modules.user.entity.User;
import media.social.modules.user.entity.UserRole;
import media.social.modules.user.entity.UserRoleId;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.RoleRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.repository.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    
    private final UserMockService userMockService;
    private final PostMockService postMockService;
    private final StoryMockService storyMockService;
    private final ConversationMockService conversationMockService;
    private final DatingMockService datingMockService;
    private final NotificationMockService notificationMockService;

    @Override
    public void run(String... args) {
        log.info("Starting DataInitializer with rich mock data and Cloudinary images...");

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).description("System Administrator").build()));

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.USER).description("Normal User").build()));

        initMissingAdmins(adminRole);

        userMockService.init(userRole);
        postMockService.init();
        storyMockService.init();
        conversationMockService.init();
        datingMockService.init();
        notificationMockService.init();

        log.info("DataInitializer finished generating all mock data successfully!");
    }

    private void initMissingAdmins(Role adminRole) {
        String[] adminUsernames = {"admin", "superadmin", "systemroot", "manager", "administrator"};
        String[] adminEmails = {"admin@gmail.com", "superadmin@gmail.com", "systemroot@gmail.com", "manager@gmail.com", "administrator@gmail.com"};
        String[] adminNames = {"Nguyễn Văn Quản Trị", "Trần Hữu Hệ Thống", "Lê Đình Admin", "Phạm Quốc Quản Lý", "Hoàng Gia Giám Sát"};

        for (int i = 0; i < 5; i++) {
            String username = adminUsernames[i];
            String email = adminEmails[i];
            String fullName = adminNames[i];

            if (userRepository.findByUsername(username).isEmpty() && !userRepository.existsByEmail(email)) {
                User admin = User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode("123456"))
                        .provider(AuthProvider.LOCAL)
                        .status(Status.ACTIVE)
                        .emailVerified(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                User savedAdmin = userRepository.save(admin);

                String avatarUrl = MockDataConstants.getRandomImageUrl();
                String coverUrl = MockDataConstants.getRandomImageUrl();

                Profile adminProfile = Profile.builder()
                        .user(savedAdmin)
                        .fullName(fullName)
                        .phone("09" + (88880000 + i))
                        .gender(Gender.MALE)
                        .city("Hà Nội")
                        .country("Việt Nam")
                        .occupation("System Administrator")
                        .company("Social Platform Core Team")
                        .education("Đại học Bách Khoa Hà Nội")
                        .avatarUrl(avatarUrl)
                        .avatarPublicId("")
                        .coverUrl(coverUrl)
                        .coverPublicId("")
                        .dateOfBirth(LocalDate.of(1990, 1, 1 + i))
                        .bio("Hệ thống quản trị viên cấp cao của nền tảng mạng xã hội và hẹn hò.")
                        .socialLinks(Map.of(
                                "facebook", "https://facebook.com/" + username,
                                "github", "https://github.com/" + username
                        ))
                        .visibility(Visibility.PUBLIC)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                profileRepository.save(adminProfile);

                userRoleRepository.save(UserRole.builder()
                        .id(new UserRoleId(savedAdmin.getId(), adminRole.getId()))
                        .user(savedAdmin)
                        .role(adminRole)
                        .assignedBy(null)
                        .assignedAt(LocalDateTime.now())
                        .build());
            }
        }
    }
}