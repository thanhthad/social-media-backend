package media.social.modults.user.dto.response.pub;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PublicUserProfileResponse {

    private Long id;

    private String username;

    private String avatarUrl;

    private String bio;

    private String fullName;

    private LocalDate dateOfBirth;

    private String gender;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

}
