package media.social.modules.user.service;

public interface EmailService {

    void sendVerificationEmail(
            String email,
            String token
    );

}