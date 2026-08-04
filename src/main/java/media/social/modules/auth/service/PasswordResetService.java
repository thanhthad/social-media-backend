package media.social.modules.auth.service;

import media.social.modules.user.entity.User;

public interface PasswordResetService {

    String createResetToken(User user);

    void resetPassword(
            String token,
            String newPassword
    );

}