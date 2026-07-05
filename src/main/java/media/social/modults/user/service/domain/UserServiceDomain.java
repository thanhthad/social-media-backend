package media.social.modults.user.service.domain;

import media.social.modults.user.entity.User;

public interface UserServiceDomain {

    public void validateUserExists(Long userId);

    public User getByUserId(Long userId);
}
