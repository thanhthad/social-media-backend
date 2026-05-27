package media.social.modults.service;

import media.social.modults.entity.User;

public interface UserServiceDomain {
    void validateUserExists (Long userId);

    User getByUserId(Long userId);
}
