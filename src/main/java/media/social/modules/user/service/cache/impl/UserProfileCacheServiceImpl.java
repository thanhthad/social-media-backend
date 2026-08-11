package media.social.modules.user.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.user.FriendshipCountResponse;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.UserProfileCacheService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileCacheServiceImpl implements UserProfileCacheService {

    private final UserRepository userRepository;


    @Cacheable(
            value = "userProfile",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    @Override
    public PublicUserProfileCacheResponse getUserProfile(Long userId) {

        return userRepository.findCurrentUserProfileCache(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );
    }

    @Cacheable(
            value = "userFollowStat",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public FriendshipCountResponse getTotalFriend(
            Long userId
    ){
        return userRepository.findFriendshipCount(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found"
                        )
                );
    }
}
