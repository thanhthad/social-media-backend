package media.social.modules.auth.service;

public interface EmailService {

    void sendVerificationEmail(
            String email,
            String token
    );

}