package media.social.modules.auth.repository;

import media.social.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken>
    findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc(
            Long userId,
            LocalDateTime now
    );

}