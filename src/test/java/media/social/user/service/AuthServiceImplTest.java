    package media.social.user.service;

    import media.social.modules.auth.entity.RefreshToken;
    import media.social.modules.auth.Enum.RoleName;
    import media.social.modules.auth.dto.request.LoginRequest;
    import media.social.modules.auth.dto.request.RegisterRequest;
    import media.social.modules.auth.dto.response.AuthResponse;
    import media.social.modules.user.entity.*;
    import media.social.modules.user.exception.user.UserAlreadyExistsException;
    import media.social.modules.user.repository.*;
    import media.social.modules.auth.security.jwt.JwtUtil;
    import media.social.modules.auth.security.userdetails.CustomUserDetails;
    import media.social.modules.user.service.RefreshTokenService;
    import media.social.modules.auth.service.impl.AuthServiceImpl;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.junit.jupiter.MockitoExtension;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.crypto.password.PasswordEncoder;

    import java.util.Collections;
    import java.util.Optional;

    import static org.assertj.core.api.Assertions.assertThat;
    import static org.junit.jupiter.api.Assertions.assertThrows;
    import static org.mockito.ArgumentMatchers.any;
    import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    public class AuthServiceImplTest {

        @Mock private AuthenticationManager authenticationManager;
        @Mock private UserRepository userRepository;
        @Mock private JwtUtil jwtUtil;
        @Mock private RefreshTokenService refreshTokenService;
        @Mock private PasswordEncoder passwordEncoder;
        @Mock private ProfileRepository profileRepository;
        @Mock private UserRoleRepository userRoleRepository;
        @Mock private RoleRepository roleRepository;

        @InjectMocks
        private AuthServiceImpl authService;

        private User testUser;
        private Role testRole;

        @BeforeEach
        void setUp() {
            testUser = User.builder().id(1L).username("testuser").email("test@mail.com").build();
            testRole = Role.builder().id(1L).name(RoleName.USER).build();
        }

        @Test
        @DisplayName("Login should return AuthResponse when credentials are valid")
        void login_Success() {
            LoginRequest request = new LoginRequest("test@mail.com", "password");
            CustomUserDetails userDetails = new CustomUserDetails(testUser.getId(), testUser.getUsername(), testUser.getEmail(), "hashed", Collections.emptyList());
            Authentication auth = mock(Authentication.class);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), any())).thenReturn("access-token");
            when(refreshTokenService.create(anyLong())).thenReturn(RefreshToken.builder().token("refresh-token").build());

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUserId()).isEqualTo(1L);
            verify(refreshTokenService).create(1L);
        }

        @Test
        @DisplayName("Register should save user, profile and user role correctly")
        void register_Success() {
            RegisterRequest request = new RegisterRequest("testuser", "test@mail.com", "password");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(testRole));

            authService.register(request);

            verify(userRepository, times(1)).save(any(User.class));
            verify(profileRepository, times(1)).save(any(Profile.class));
            verify(userRoleRepository, times(1)).save(any(UserRole.class));
        }

        @Test
        @DisplayName("Register should throw UserAlreadyExistsException when username exists")
        void register_ThrowsException_WhenUserExists() {
            RegisterRequest request = new RegisterRequest("testuser", "test@mail.com", "password");
            when(userRepository.existsByUsername(anyString())).thenReturn(true);

            assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Logout should call refresh token service revoke")
        void logout_CallsRevoke() {
            String token = "dummy-refresh-token";
            authService.logout(token);
            verify(refreshTokenService, times(1)).revoke(token);
        }
    }