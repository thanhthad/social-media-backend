package media.social.modults.user.dto.response.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PublicUserProfileCacheResponse {


    private Long id;

    private String username;

    private String avatarUrl;

    private String bio;

    private String fullName;

    private LocalDate dateOfBirth;

    private String gender;

    private String location;

    private Long totalFollower;

    private Long totalFollowing;

}