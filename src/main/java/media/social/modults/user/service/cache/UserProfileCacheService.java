package media.social.modults.user.service.cache;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.dto.response.user.PublicUserProfileCacheResponse;
import media.social.modults.user.dto.response.user.FollowCountResponse;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.profile.ProfileNotFoundException;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.repository.ProfileRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.service.FollowService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileCacheService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final FollowService followService;

    @Cacheable(
            value = "userProfile",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public PublicUserProfileCacheResponse getProfile(
            Long userId
    ){
        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () -> new UserNotFoundException(
                                        "User not found"
                                )
                        );
        Profile profile =
                profileRepository.findByUserId(userId)
                        .orElseThrow(
                                () -> new ProfileNotFoundException(
                                        "Profile not found"
                                )
                        );
        FollowCountResponse count =
                followService.getProfile(userId);
        return PublicUserProfileCacheResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .fullName(profile.getFullName())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .location(profile.getLocation())
                .totalFollower(count.getTotal_follower())
                .totalFollowing(count.getTotal_following())
                .build();
    }
}