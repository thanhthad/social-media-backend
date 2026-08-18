package media.social.modules.auth.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.auth.dto.request.*;
import media.social.modules.auth.entity.RefreshToken;
import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.exception.password.AccountAlreadyLockedException;
import media.social.modules.auth.exception.verification.EmailAlreadyVerifiedException;
import media.social.modules.auth.service.PasswordResetService;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.user.exception.user.ForbiddenException;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.user.entity.*;
import media.social.modules.user.exception.role.RoleNotFoundException;
import media.social.modules.user.exception.user.UnauthorizedException;
import media.social.modules.user.exception.user.UserAlreadyExistsException;
import media.social.modules.auth.dto.response.AuthResponse;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.RoleRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.repository.UserRoleRepository;
import media.social.modules.auth.security.jwt.JwtUtil;
import media.social.modules.auth.security.userdetails.CustomUserDetails;
import media.social.modules.auth.service.AuthService;
import media.social.modules.auth.service.EmailVerificationService;
import media.social.modules.user.service.RefreshTokenService;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
    private final PasswordResetService passwordResetService;
    private final UserServiceDomain userServiceDomain;
    private final DatingProfileRepository datingProfileRepository;

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
            userServiceDomain.increaseFailedAttempt(
                    request.getEmail()
            );

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

        userServiceDomain.resetFailedAttempt(
                user.getId()
        );

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
                .createdAt(OffsetDateTime.now())
                .build();

        DatingProfile datingProfile = DatingProfile.builder()
                .user(user)
                .build();

        datingProfileRepository.save(datingProfile);

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
    @Transactional
    public AuthResponse generateAccessToken(String refreshToken) {

        RefreshToken oldToken =
                refreshTokenService.verify(refreshToken);

        User user = oldToken.getUser();

        if(user.getStatus().equals(Status.BANNED)){
            throw new AccessDeniedException(
                    "Your account is banned"
            );
        }
        refreshTokenService.revoke(refreshToken);

        RefreshToken newRefreshToken =
                refreshTokenService.create(
                        user.getId()
                );

        String accessToken =
                refreshTokenService.generateAccessToken(
                        newRefreshToken.getToken()
                );
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .hasUsername(
                        user.getUsername() != null
                )
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Override
    public String forgotPassword(
            ForgotPasswordRequest request
    ){
        User user =
                userRepository.findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(
                                () -> new UserNotFoundException(
                                        "User not found"
                                )
                        );

        String token = passwordResetService
                .createResetToken(user);
        return token;
    }

    @Override
    public void resetPassword(
            ResetPasswordRequest request
    ){
        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );
    }

    @Override
    @Transactional
    public void resendVerification(
            ResendVerificationRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found"
                        )
                );

        if(user.getEmailVerified()) {
            throw new EmailAlreadyVerifiedException(
                    "Email already verified"
            );
        }

        emailVerificationService
                .deleteByUser(user);

        emailVerificationService
                .createVerificationToken(user);
    }
}
