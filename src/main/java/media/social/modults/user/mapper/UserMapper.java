package media.social.modults.user.mapper;

import media.social.modults.user.dto.response.self.ProfileResponse;
import media.social.modults.user.dto.response.self.UserProfileResponse;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toUserProfileResponse(
            User user,
            Profile profile
    ) {

        ProfileResponse profileResponse = null;

        if (profile != null) {
            profileResponse = ProfileResponse.builder()
                    .id(profile.getId())
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
                .role(user.getRole())
                .status(user.getStatus())
                .profile(profileResponse)
                .build();
    }
}