package media.social.modules.auth.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.auth.entity.RefreshToken;
import media.social.modules.auth.enums.Status;
import media.social.modules.user.exception.user.ForbiddenException;
import media.social.modules.auth.enums.RoleName;
import media.social.modules.user.entity.*;
import media.social.modules.user.exception.role.RoleNotFoundException;
import media.social.modules.user.exception.user.UnauthorizedException;
import media.social.modules.user.exception.user.UserAlreadyExistsException;
import media.social.modules.auth.dto.request.LoginRequest;
import media.social.modules.auth.dto.request.RegisterRequest;
import media.social.modules.auth.dto.response.AuthResponse;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.RoleRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.repository.UserRoleRepository;
import media.social.modules.auth.security.jwt.JwtUtil;
import media.social.modules.auth.security.userdetails.CustomUserDetails;
import media.social.modules.auth.service.AuthService;
import media.social.modules.auth.service.EmailVerificationService;
import media.social.modules.user.service.RefreshTokenService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationService emailVerificationService;

    @Override
    public AuthResponse login(LoginRequest request) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {

            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        } catch (InternalAuthenticationServiceException e) {

            if (e.getCause() instanceof DisabledException) {

                throw new ForbiddenException(
                        e.getCause().getMessage()
                );
            }
            throw e;
        }

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthorities()
        );

        String refreshToken =
                refreshTokenService.create(user.getId())
                        .getToken();

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        if(userRepository.existsByUsername(request.getUsername())){
            throw new UserAlreadyExistsException("User already exists with username");
        }
        if(userRepository.existsByEmail(request.getEmail())){
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }
        Role role = roleRepository.findByName(RoleName.USER).orElseThrow(
                () -> new RoleNotFoundException("Role not found")
        );

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        User saved = userRepository.save(user);

        Profile profile = Profile.builder()
                .user(saved)
                .createdAt(LocalDateTime.now())
                .build();
        profileRepository.save(profile);

        UserRole userRole =UserRole.builder()
                .id(new UserRoleId(user.getId(),role.getId()))
                .role(role)
                .user(saved)
                .assignedAt(LocalDateTime.now())
                .assignedBy(null)
                .build();
        userRoleRepository.save(userRole);

        emailVerificationService.createVerificationToken(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse generateAccessToken(String refreshToken) {

        RefreshToken token = refreshTokenService.verify(refreshToken);

        String accessToken = refreshTokenService.generateAccessToken(refreshToken);

        User user = token.getUser();

        if(user.getStatus().equals(Status.BANNED)){
            throw new AccessDeniedException("Your account is banned");
        }

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
