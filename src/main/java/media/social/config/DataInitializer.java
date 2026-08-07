package media.social.config;

import lombok.RequiredArgsConstructor;
import media.social.modules.post.dto.request.post.CreatePostRequest;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.service.PostService;
import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.user.entity.*;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.repository.*;
import media.social.modules.auth.security.userdetails.CustomUserDetailsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostService postService;
    private final PostRepository postRepository;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void run(String... args) {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).description("System Administrator").build()));

        Role userRoleName = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.USER).description("Normal User").build()));

        initMissingAdmins(adminRole);
        initMissingUsers(userRoleName);
        initPosts();
    }

    private void initPosts() {
        if (postRepository.count() > 0) {
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return;
        }

        String[] sampleContents = {
                "Hôm nay thời tiết thật đẹp để ngồi code java và uống cà phê sách #java #coding #coffee",
                "Mới học được cách tối ưu câu lệnh truy vấn PostgreSQL cực kỳ mượt mà #database #postgresql #dev",
                "Có ai đang thức khuya fix bug giống tôi không vậy #bugs #codinglife #night",
                "Chia sẻ một chút kinh nghiệm học lập trình web fullstack cho người mới bắt đầu #fullstack #webdev #tutorial",
                "Cuối tuần rồi anh em có kèo đi chơi đâu chưa hay lại cắm mặt vào máy tính #weekend #relax #it",
                "Spring Boot đúng là một framework mạnh mẽ và tiện lợi cho backend #springboot #java #backend",
                "Đang làm dự án mạng xã hội bằng React và Spring Boot, mọi người góp ý nhé #reactjs #socialnetwork #project",
                "Học tiếng Nhật mỗi ngày để chuẩn bị cho kỳ thi sắp tới #japanese #studying #goal",
                "Cuộc sống giống như một dòng code, nếu có lỗi thì phải sửa từ từ #quotes #life #motivation",
                "Vừa deploy thành công ứng dụng lên cloud, cảm giác thật tuyệt vời #devops #cloud #success"
        };

        Random random = new Random();

        for (User user : users) {
            if (user.getId() < 6 || user.getId() > 105) {
                continue;
            }
            mockLogin(user);

            int postCount = 5 + random.nextInt(6);

            for (int i = 0; i < postCount; i++) {
                String content = sampleContents[random.nextInt(sampleContents.length)];

                Visibility visibility;
                int vRand = random.nextInt(100);
                if (vRand < 50) {
                    visibility = Visibility.PUBLIC;
                } else if (vRand < 85) {
                    visibility = Visibility.FOLLOWERS;
                } else {
                    visibility = Visibility.PRIVATE;
                }

                CreatePostRequest request = CreatePostRequest.builder()
                        .content(content)
                        .visibility(visibility)
                        .files(null)
                        .build();

                try {
                    postService.createPost(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        SecurityContextHolder.clearContext();
    }

    private void mockLogin(User user) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
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

                Profile adminProfile = Profile.builder()
                        .user(savedAdmin)
                        .fullName(fullName)
                        .phone("09" + (88880000 + i))
                        .gender(Gender.MALE)
                        .city("Hà Nội")
                        .dateOfBirth(LocalDate.of(1990, 1, 1 + i))
                        .bio("Hệ thống quản trị viên cấp cao của nền tảng.")
                        .socialLinks(Map.of())
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

    private void initMissingUsers(Role userRole) {
        String[] hovakhat = {
                "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
                "Bùi", "Đỗ", "Hồ", "Ngô", "Sơn", "Dương", "Lý", "Đinh", "Đoàn", "Lương"
        };

        String[] tendem = {
                "Văn", "Thị", "Hữu", "Đình", "Gia", "Minh", "Quốc", "Thanh", "Ngọc", "Xuân",
                "Hồng", "Tuấn", "Mỹ", "Đức", "Công", "Bảo", "Kim", "Hoài", "Phương", "Diệu"
        };

        String[] ten = {
                "Anh", "Bình", "Cường", "Dũng", "Đạt", "Giang", "Hải", "Hòa", "Hùng", "Hương",
                "Khánh", "Linh", "Long", "Nam", "Nga", "Phong", "Phúc", "Phương", "Quân", "Sơn",
                "Thảo", "Thắng", "Thu", "Trang", "Trung", "Tú", "Tùng", "Uyên", "Vân", "Việt",
                "An", "Bảo", "Chi", "Dương", "Đức", "Hà", "Hân", "Hiếu", "Huy", "Khang",
                "Ngân", "Nhi", "Phát", "Quỳnh", "Tâm", "Thành", "Thiện", "Thịnh", "Thy", "Trâm"
        };

        String[] usernamePrefixes = {
                "coder", "dev", "gamer", "tech", "pro", "master", "neo", "zoro", "luffy", "it",
                "boss", "ninja", "samurai", "ghost", "shadow", "cyber", "pixel", "matrix", "hacker", "creator"
        };

        String[] emailDomains = {
                "gmail.com", "yahoo.com", "outlook.com", "icloud.com", "fpt.edu.vn", "bk.edu.vn"
        };

        String[] locations = {
                "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ", "Nha Trang",
                "Đà Lạt", "Huế", "Vũng Tàu", "Bình Dương", "Biên Hòa", "Quảng Ninh",
                "Bắc Ninh", "Thái Nguyên", "Nam Định", "Quy Nhơn", "Buôn Ma Thuột", "Vinh"
        };

        Gender[] genders = {Gender.MALE, Gender.FEMALE, Gender.OTHER};

        String[] bios = {
                "Yêu công nghệ và thích khám phá điều mới.",
                "Đam mê code dạo và nghe nhạc lofi mỗi tối.",
                "Sống chậm lại, yêu thương nhiều hơn mỗi ngày.",
                "Đang trên con đường trở thành Fullstack Developer chuyên nghiệp.",
                "Cà phê sách là năng lượng khởi đầu ngày mới.",
                "Làm hết sức, chơi hết mình, sống trọn đam mê.",
                "Tìm kiếm những người bạn cùng tần số để chia sẻ kiến thức.",
                "Chỉ là một chiếc coder bình thường thích ăn vặt.",
                "Hôm nay bạn thế nào? Cùng chia sẻ câu chuyện nhé!",
                "Đọc sách, viết code và tận hưởng cuộc sống.",
                "Sống là không chờ đợi, hãy cứ làm những gì mình thích.",
                "Tươi cười là cách vượt qua mọi giông bão."
        };

        Random random = new Random();

        for (int i = 1; i <= 100; i++) {
            String firstName = ten[random.nextInt(ten.length)].toLowerCase();
            String prefix = usernamePrefixes[random.nextInt(usernamePrefixes.length)];
            String username = prefix + "_" + firstName + random.nextInt(900 + 100);

            String domain = emailDomains[random.nextInt(emailDomains.length)];
            String email = username + "@" + domain;

            if (!userRepository.existsByUsername(username) && !userRepository.existsByEmail(email)) {

                String fullName = hovakhat[random.nextInt(hovakhat.length)] + " " +
                        tendem[random.nextInt(tendem.length)] + " " +
                        ten[random.nextInt(ten.length)];

                String phone = "09" + (10000000 + random.nextInt(90000000));
                Gender gender = genders[random.nextInt(genders.length)];
                String location = locations[random.nextInt(locations.length)];
                String bio = bios[random.nextInt(bios.length)];

                int year = 1992 + random.nextInt(12);
                int dayOfYear = 1 + random.nextInt(364);
                LocalDate dob = LocalDate.ofYearDay(year, dayOfYear);

                User user = User.builder()
                        .username(username)
                        .email(email)
                        .emailVerified(true)
                        .passwordHash(passwordEncoder.encode("123456"))
                        .provider(AuthProvider.LOCAL)
                        .status(Status.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                User savedUser = userRepository.save(user);

                Profile profile = Profile.builder()
                        .user(savedUser)
                        .fullName(fullName)
                        .phone(phone)
                        .gender(gender)
                        .city(location)
                        .dateOfBirth(dob)
                        .bio(bio)
                        .socialLinks(Map.of())
                        .visibility(Visibility.PUBLIC)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                profileRepository.save(profile);

                UserRole uRole = UserRole.builder()
                        .id(new UserRoleId(savedUser.getId(), userRole.getId()))
                        .user(savedUser)
                        .role(userRole)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userRoleRepository.save(uRole);
            }
        }
    }
}