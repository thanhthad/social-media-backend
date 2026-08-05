package media.social.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.entity.PasswordResetToken;
import media.social.modules.auth.exception.password.PasswordResetTokenExpiredException;
import media.social.modules.auth.exception.password.PasswordResetTokenInvalidException;
import media.social.modules.auth.exception.password.PasswordResetTokenUsedException;
import media.social.modules.auth.repository.PasswordResetTokenRepository;
import media.social.modules.auth.service.EmailService;
import media.social.modules.auth.service.PasswordResetService;
import media.social.modules.user.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl
        implements PasswordResetService {

    private final PasswordResetTokenRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public String createResetToken(User user){

        repository.deleteByUser(user);

        String token =
                UUID.randomUUID().toString();

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .user(user)
                        .token(token)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(15)
                        )
                        .build();

        repository.save(resetToken);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                token
        );
        return token;
    }

    @Override
    @Transactional
    public void resetPassword(
            String token,
            String newPassword
    ){

        PasswordResetToken resetToken =
                repository.findByTokenWithUser(token)
                        .orElseThrow(
                                () -> new PasswordResetTokenInvalidException(
                                        "Invalid token")
                        );

        if(resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())){

            throw new PasswordResetTokenExpiredException(
                    "Token expired"
            );
        }

        if(resetToken.getUsed()){

            throw new PasswordResetTokenUsedException(
                    "Token already used"
            );
        }

        User user =
                resetToken.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(
                        newPassword
                )
        );
        resetToken.setUsed(true);
        repository.save(resetToken);
    }

}
