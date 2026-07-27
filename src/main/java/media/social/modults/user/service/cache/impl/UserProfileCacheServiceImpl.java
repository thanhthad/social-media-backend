package media.social.modults.user.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.service.cache.UserProfileCacheService;
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

        PublicUserProfileCacheResponse user = userRepository.findCurrentUserProfileCache(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        return PublicUserProfileCacheResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalFollower(user.getTotalFollower())
                .totalFollowing(user.getTotalFollowing())
                .build();

    }
}
