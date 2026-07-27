package media.social.modults.user.service.cache;


import media.social.modults.user.dto.response.cache.PublicUserProfileCacheResponse;

public interface UserProfileCacheService {

    PublicUserProfileCacheResponse getUserProfile(
            Long userId
    );

}