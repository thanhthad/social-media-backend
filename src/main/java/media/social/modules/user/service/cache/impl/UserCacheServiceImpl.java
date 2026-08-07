package media.social.modules.user.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.user.dto.response.cache.UserCacheResponse;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.UserCacheService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCacheServiceImpl implements UserCacheService {

    private final CacheManager cacheManager;
    private final UserRepository userRepository;

    @Override
    @CacheEvict(
            value = "userProfile",
            key = "#userId"
    )
    public void evictProfile(Long userId) {

    }

    @Override
    @CacheEvict(
            value = "userFollowStat",
            key = "#userId"
    )
    public void evict(Long userId) {

    }

    @Override
    @Cacheable(
            value = "users",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public UserCacheResponse getUser(Long userId) {


        User user =
                userRepository.findByIdWithProfile(userId)
                        .orElseThrow(
                                () -> new UserNotFoundException(
                                        "User not found"
                                )
                        );


        return UserCacheResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullname(user.getProfile().getFullName())
                .avatarUrl(
                        user.getProfile()
                                .getAvatarUrl()
                )
                .build();
    }

}
