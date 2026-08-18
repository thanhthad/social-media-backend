package media.social.modules.dating.service;

import media.social.modules.user.entity.User;

public interface DatingMatchService {

    void createMatch(User userOne, User userTwo);
}
