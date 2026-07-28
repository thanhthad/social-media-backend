package media.social.modults.user.service.cache;


import media.social.modults.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modults.user.dto.response.cache.UserFollowStatCacheResponse;

public interface UserProfileCacheService {

    PublicUserProfileCacheResponse getUserProfile(
            Long userId
    );

    UserFollowStatCacheResponse getFollowStat(
            Long userId
    );

}