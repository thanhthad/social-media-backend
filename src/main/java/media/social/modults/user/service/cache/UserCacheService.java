package media.social.modults.user.service.cache;

import media.social.modults.user.dto.response.cache.UserCacheResponse;


public interface UserCacheService {

    UserCacheResponse getUser(Long userId);

    void evictProfile(Long id);

    void evict(Long userId);
}