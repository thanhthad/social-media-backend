package media.social.modults.user.dto.response.cache;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PublicUserProfileCacheResponse {

    private Long id;

    private String username;

    private String email;

    private String avatarUrl;

    private String bio;

    private String fullName;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long totalFollower;

    private Long totalFollowing;
}