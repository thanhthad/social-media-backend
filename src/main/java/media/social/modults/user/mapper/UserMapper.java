package media.social.modults.user.mapper;

import media.social.modults.user.dto.response.cache.PublicUserProfileCacheResponse;
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
            PublicUserProfileCacheResponse user
    ) {

        ProfileResponse profileResponse = profileResponse = ProfileResponse.builder()
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();;

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profile(profileResponse)
                .totalFollower(user.getTotalFollower())
                .totalFollowing(user.getTotalFollowing())
                .build();
    }

}