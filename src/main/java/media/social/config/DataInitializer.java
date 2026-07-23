package media.social.config;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.Enum.Status;
import media.social.modults.user.entity.Role;
import media.social.modults.user.entity.User;
import media.social.modults.user.entity.UserRole;
import media.social.modults.user.entity.UserRoleId;
import media.social.modults.user.repository.RoleRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.repository.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {

                    Role role = Role.builder()
                            .name(RoleName.ADMIN)
                            .build();

                    return roleRepository.save(role);
                });

        if (userRepository.findByUsername("admin").isEmpty()
                && !userRepository.existsByEmail("admin@gmail.com"))
        {


            User admin = User.builder()
                    .username("admin")
                    .email("admin@gmail.com")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .status(Status.ACTIVE)
                    .build();

            User savedAdmin = userRepository.save(admin);

            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(savedAdmin.getId(),adminRole.getId()))
                    .user(admin)
                    .role(adminRole)
                    .assignedBy(null)
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
        }
    }
}