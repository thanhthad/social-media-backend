package media.social.modules.user.mapper;

import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.cache.UserFollowStatCacheResponse;
import media.social.modules.user.dto.response.user.ProfileResponse;
import media.social.modules.user.dto.response.user.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

//    public UserProfileResponse toUserProfileResponse(
//            PublicUserProfileCacheResponse user,
//            UserFollowStatCacheResponse userFollowStat
//
//    ) {
//
//        ProfileResponse profileResponse = profileResponse = ProfileResponse.builder()
//                .fullName(user.getFullName())
//                .avatarUrl(user.getAvatarUrl())
//                .bio(user.getBio())
//                .phone(user.getPhone())
//                .dateOfBirth(user.getDateOfBirth())
//                .gender(user.getGender())
//                .location(user.getLocation())
//                .createdAt(user.getCreatedAt())
//                .updatedAt(user.getUpdatedAt())
//                .build();;
//
//        return UserProfileResponse.builder()
//                .id(user.getId())
//                .username(user.getUsername())
//                .email(user.getEmail())
//                .profile(profileResponse)
//                .totalFollower(userFollowStat.getTotalFollower())
//                .totalFollowing(userFollowStat.getTotalFollowing())
//                .build();
//    }

}