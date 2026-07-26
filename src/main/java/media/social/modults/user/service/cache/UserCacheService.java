package media.social.modults.user.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final CacheManager cacheManager;

    public void evictProfile(Long userId){

        Cache cache =
                cacheManager.getCache("userProfile");


        if(cache != null){
            cache.evict(userId);
        }
    }
}