package media.social.modules.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modules.auth.entity.RefreshToken;
import media.social.modules.user.entity.Role;
import media.social.modules.user.entity.User;
import media.social.modules.user.entity.UserRole;
import media.social.modules.auth.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modules.auth.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modules.auth.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modules.auth.repository.RefreshTokenRepository;
import media.social.modules.auth.security.jwt.JwtUtil;
import media.social.modules.user.service.RefreshTokenService;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Log4j2
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserServiceDomain userServiceDomain;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;



    // ================= CREATE =================
    @Override
    public RefreshToken create(Long userId) {

        User user = userServiceDomain.getByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiredAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        log.info("AUTH_EVENT | action=REFRESH_TOKEN_CREATED | userId={} | tokenId={}",
                user.getId(), saved.getId());

        return saved;
    }

    // ================= VERIFY =================
    @Override
    public RefreshToken verify(String token) {

        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException("Refresh token not found")
                        );

        if (refreshToken.isRevoked()) {
            throw new RefreshTokenRevokedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        return refreshToken;
    }

    // ================= FIND VALID TOKEN =================
    @Override
    @Transactional
    public RefreshToken findValidByUser(Long userId) {

        return refreshTokenRepository
                .findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc(
                        userId,
                        LocalDateTime.now()
                )
                .map(token -> {
                    log.info("AUTH_EVENT | action=REFRESH_TOKEN_REUSE | userId={} | tokenId={}",
                            userId, token.getId());
                    return token;
                })
                .orElseGet(() -> {
                    return create(userId);
                });
    }

    // ================= GENERATE ACCESS TOKEN =================
    @Override
    public String generateAccessToken(String refreshTokenValue) {

        RefreshToken refreshToken = verify(refreshTokenValue);
        User user = refreshToken.getUser();

        List<SimpleGrantedAuthority> authorities =
                user.getUserRoles()
                        .stream()
                        .map(UserRole::getRole)
                        .map(Role::getName)
                        .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                        .toList();

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                authorities
        );

        log.info("AUTH_EVENT | action=ACCESS_TOKEN_REFRESH | userId={} | tokenId={}",
                user.getId(), refreshToken.getId());

        return accessToken;
    }

    // ================= REVOKE =================
    @Transactional
    @Override
    public void revoke(String refreshToken) {

        RefreshToken token = verify(refreshToken);
        token.setRevoked(true);

        log.info("AUTH_EVENT | action=REFRESH_TOKEN_REVOKED | userId={} | tokenId={}",
                token.getUser().getId(), token.getId());
    }
}