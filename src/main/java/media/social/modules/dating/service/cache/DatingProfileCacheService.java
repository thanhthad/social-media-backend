package media.social.modules.dating.service.cache;

import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;

public interface DatingProfileCacheService {

    DatingProfileCacheResponse getDatingProfile(Long userId);

    void evictProfile(Long userId);
}
