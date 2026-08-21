package media.social.modules.user.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.user.dto.projection.FriendshipCountProjection;
import media.social.modules.user.dto.projection.PublicUserProfileCacheProjection;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.friend.FriendshipCountResponse;
import media.social.modules.user.enums.FriendshipStatus;
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
            value = "friendShipCount",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public FriendshipCountResponse getTotalFriend(
            Long userId
    ){
        FriendshipCountProjection friendshipCountProjection = userRepository.findFriendshipCount(userId, FriendshipStatus.ACCEPTED)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found"
                        )
                );

        return FriendshipCountResponse.builder()
                .totalFriends(friendshipCountProjection.getTotalFriends())
                .build();
    }
}
