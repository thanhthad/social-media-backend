package media.social.modules.user.service.cache;


import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.cache.UserFollowStatCacheResponse;

public interface UserProfileCacheService {

    PublicUserProfileCacheResponse getUserProfile(
            Long userId
    );

    UserFollowStatCacheResponse getFollowStat(
            Long userId
    );

}