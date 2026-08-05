package media.social.modules.user.service.domain;

import media.social.modules.user.entity.User;

public interface UserServiceDomain {

    public void validateUserExists(Long userId);

    public User getByUserId(Long userId);

    User getByEmail(String email);

    void increaseFailedAttempt(
            String email
    );

    void resetFailedAttempt(
            Long userId
    );

    boolean isAccountLocked(
            User user
    );
}
