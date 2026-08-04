package media.social.modules.auth.service;

import media.social.modules.user.entity.User;

public interface EmailVerificationService {

    void createVerificationToken(User user);

    void verify(String token);

}