package media.social.modules.user.service.cache;

import media.social.modules.user.dto.response.cache.UserCacheResponse;


public interface UserCacheService {

    UserCacheResponse getUser(Long userId);

}