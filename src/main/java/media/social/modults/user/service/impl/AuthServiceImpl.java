package media.social.modults.user.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.RefreshToken;
import media.social.modults.user.exception.user.UserAlreadyExistsException;
import media.social.modults.user.dto.request.auth.LoginRequest;
import media.social.modults.user.dto.request.auth.RegisterRequest;
import media.social.modults.user.dto.response.AuthResponse;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.repository.ProfileRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.security.jwt.JwtUtil;
import media.social.modults.user.service.AuthService;
import media.social.modults.user.service.RefreshTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);

        Profile profile = Profile.builder()
                .user(user)
                .build();
        profileRepository.save(profile);

        String refreshToken = refreshTokenService.create(user.getId()).getToken();
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new UserNotFoundException("User not found with email : " + request.getEmail())
        );
        if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
            throw new BadCredentialsException("Invalid password");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String refreshToken = refreshTokenService.create(user.getId()).getToken();
        String accessToken = jwtUtil.generateAccessToken(user.getId(),user.getUsername(), user.getEmail(),user.getRole());
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse generateAccessToken(String refreshToken) {

        String accessToken = refreshTokenService.generateAccessToken(refreshToken);

        RefreshToken token = refreshTokenService.verify(refreshToken);
        User user = token.getUser();

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
