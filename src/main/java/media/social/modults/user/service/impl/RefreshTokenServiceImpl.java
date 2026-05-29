package media.social.modults.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.user.entity.RefreshToken;
import media.social.modults.entity.User;
import media.social.modults.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modults.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modults.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modults.repository.RefreshTokenRepository;
import media.social.modults.user.security.jwt.JwtUtil;
import media.social.modults.user.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public RefreshToken create(Long userId) {
        User user = userServiceDomain.getByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(refreshExpirationMs))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        log.info("AUTH_EVENT | action=REFRESH_TOKEN_CREATED | userId={} | tokenId={}",
                user.getId(), saved.getId());

        return saved;
    }

    // ================= VERIFY =================
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

    // ================= GENERATE NEW ACCESS TOKEN =================
    @Transactional
    public String generateAccessToken(String refreshToken) {

        RefreshToken token = verify(refreshToken);
        User user = token.getUser();

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );

        log.info("AUTH_EVENT | action=ACCESS_TOKEN_REFRESH | userId={} | tokenId={}",
                user.getId(), token.getId());

        return accessToken;
    }

    // ================= REVOKE =================
    public void revoke(String refreshToken) {

        RefreshToken token = verify(refreshToken);
        token.setRevoked(true);

        refreshTokenRepository.save(token);

        log.info("AUTH_EVENT | action=REFRESH_TOKEN_REVOKED | userId={} | tokenId={}",
                token.getUser().getId(), token.getId());
    }
}