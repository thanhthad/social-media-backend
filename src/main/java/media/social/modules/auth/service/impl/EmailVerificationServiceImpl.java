package media.social.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.entity.EmailVerificationToken;
import media.social.modules.user.entity.User;
import media.social.modules.auth.exception.verification.EmailVerificationTokenExpiredException;
import media.social.modules.auth.exception.verification.EmailVerificationTokenInvalidException;
import media.social.modules.auth.exception.verification.EmailVerificationTokenUsedException;
import media.social.modules.auth.repo.EmailVerificationTokenRepository;
import media.social.modules.auth.service.EmailService;
import media.social.modules.auth.service.EmailVerificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl
        implements EmailVerificationService {

    private final EmailVerificationTokenRepository repository;
    private final EmailService emailService;

    @Override
    public void createVerificationToken(User user) {

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .user(user)
                        .token(token)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusHours(24)
                        )
                        .build();

        repository.save(verificationToken);

        emailService.sendVerificationEmail(
                user.getEmail(),
                token
        );
    }

    @Override
    @Transactional
    public void verify(String token) {

        EmailVerificationToken verificationToken =
                repository.findByToken(token)
                        .orElseThrow(
                                () -> new EmailVerificationTokenInvalidException(
                                        "Invalid verification token"
                                )
                        );

        if(verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new EmailVerificationTokenExpiredException(
                    "Verification token expired"
            );
        }

        if(Boolean.TRUE.equals(
                verificationToken.getUsed()
        )){

            throw new EmailVerificationTokenUsedException(
                    "Verification token already used"
            );
        }

        User user = verificationToken.getUser();

        user.setEmailVerified(true);

        verificationToken.setUsed(true);

        repository.save(verificationToken);

    }
}