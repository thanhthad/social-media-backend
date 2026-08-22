package media.social.modules.user.service.cache;

import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.friend.FriendshipCountResponse;
import media.social.modules.user.dto.response.friend.MutualFriendCountResponse;

import java.util.List;

public interface UserProfileCacheService {

    PublicUserProfileCacheResponse getUserProfile(
            Long userId
    );

    FriendshipCountResponse getTotalFriend(
            Long userId
    );

    MutualFriendCountResponse getTotalMutualFriend(
            Long currentUserId,
            Long targetUserId
    );

    List<String> getMutualFriendAvatars(
            Long currentUserId,
            Long targetUserId
    );

    void evictProfile(Long id);

    void evictFriendShipCount(Long userId);

}