package media.social.modules.user.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.infrastructure.redis.RedisCacheNames;
import media.social.modules.user.dto.projection.FriendshipCountProjection;
import media.social.modules.user.dto.projection.MutualFriendCountProjection;
import media.social.modules.user.dto.projection.PublicUserProfileCacheProjection;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.friend.FriendshipCountResponse;
import media.social.modules.user.dto.response.friend.MutualFriendCountResponse;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.FriendshipRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.UserProfileCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileCacheServiceImpl implements UserProfileCacheService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    @CacheEvict(
            value = RedisCacheNames.USER_PROFILE,
            key = "#userId"
    )
    public void evictProfile(Long userId) {

    }

    @Override
    @CacheEvict(
            value = RedisCacheNames.FRIENDSHIP_COUNT,
            key = "#userId"
    )
    public void evictFriendShipCount(Long userId) {

    }

    @Cacheable(
            value = RedisCacheNames.USER_PROFILE,
            key = "#userId"
    )
    @Transactional(readOnly = true)
    @Override
    public PublicUserProfileCacheResponse getUserProfile(Long userId) {

        PublicUserProfileCacheProjection projection =
                userRepository.findCurrentUserProfileCache(userId)
                        .orElseThrow(
                                () -> new UserNotFoundException("User not found")
                        );

        return PublicUserProfileCacheResponse.builder()
                .id(projection.getId())
                .email(projection.getEmail())
                .username(projection.getUsername())
                .avatarUrl(projection.getAvatarUrl())
                .coverUrl(projection.getCoverUrl())
                .bio(projection.getBio())
                .fullName(projection.getFullName())
                .website(projection.getWebsite())
                .phone(projection.getPhone())
                .dateOfBirth(projection.getDateOfBirth())
                .gender(projection.getGender())
                .country(projection.getCountry())
                .city(projection.getCity())
                .district(projection.getDistrict())
                .occupation(projection.getOccupation())
                .company(projection.getCompany())
                .education(projection.getEducation())
                .visibility(projection.getVisibility())
                .socialLinks(projection.getSocialLinks())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }

    @Cacheable(
            value = RedisCacheNames.FRIENDSHIP_COUNT,
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public FriendshipCountResponse getTotalFriend(
            Long userId
    ){
        FriendshipCountProjection friendshipCountProjection =
                friendshipRepository.findFriendshipCount(userId, FriendshipStatus.ACCEPTED)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found"
                        )
                );

        return FriendshipCountResponse.builder()
                .totalFriends(friendshipCountProjection.getTotalFriends())
                .build();
    }

    @Cacheable(
            value = RedisCacheNames.MUTUAL_FRIEND_COUNT,
            key = "#currentUserId + ':' + #targetUserId"
    )
    @Transactional(readOnly = true)
    public MutualFriendCountResponse getTotalMutualFriend(
            Long currentUserId,
            Long targetUserId
    ) {
        MutualFriendCountProjection projection =
                friendshipRepository.findMutualFriendCount(
                                currentUserId,
                                targetUserId
                        )
                        .orElseThrow(
                                () -> new UserNotFoundException(
                                        "User not found"
                                )
                        );

        return MutualFriendCountResponse.builder()
                .totalMutualCount(
                        projection.getTotalMutualFriends()
                )
                .build();
    }

    @Cacheable(
            value = RedisCacheNames.MUTUAL_FRIEND_AVATARS,
            key = "#currentUserId + ':' + #targetUserId"
    )
    @Transactional(readOnly = true)
    public List<String> getMutualFriendAvatars(
            Long currentUserId,
            Long targetUserId
    ) {
        return friendshipRepository.findMutualFriendAvatars(
                currentUserId,
                targetUserId
        );
    }

}
