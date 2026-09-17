package media.social.modules.auth.repository;

import media.social.modules.auth.entity.PasswordResetToken;
import media.social.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @Query("""
    SELECT prt
    FROM PasswordResetToken prt
    JOIN FETCH prt.user
    WHERE prt.token = :token
""")
    Optional<PasswordResetToken> findByTokenWithUser(
            @Param("token") String token
    );

    void deleteByUser(User user);
}