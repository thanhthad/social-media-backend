package media.social.modults.user.mapper;

import media.social.modults.user.dto.response.user.FollowCountResponse;
import media.social.modults.user.dto.response.user.UserSearchResponse;
import media.social.modults.user.dto.response.user.ProfileResponse;
import media.social.modults.user.dto.response.user.UserProfileResponse;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toUserProfileResponse(
            User user,
            Profile profile,
            FollowCountResponse followCount
    ) {

        ProfileResponse profileResponse = null;

        if (profile != null) {
            profileResponse = ProfileResponse.builder()
                    .fullName(profile.getFullName())
                    .avatarUrl(profile.getAvatarUrl())
                    .bio(profile.getBio())
                    .phone(profile.getPhone())
                    .dateOfBirth(profile.getDateOfBirth())
                    .gender(profile.getGender())
                    .location(profile.getLocation())
                    .createdAt(profile.getCreatedAt())
                    .updatedAt(profile.getUpdatedAt())
                    .build();
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profile(profileResponse)
                .totalFollower(followCount.getTotal_follower())
                .totalFollowing(followCount.getTotal_following())
                .build();
    }

}