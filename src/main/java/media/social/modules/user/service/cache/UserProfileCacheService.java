package media.social.modules.user.service.cache;

import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.user.FriendshipCountResponse;

public interface UserProfileCacheService {

    PublicUserProfileCacheResponse getUserProfile(
            Long userId
    );

    FriendshipCountResponse getTotalFriend(
            Long userId
    );

}