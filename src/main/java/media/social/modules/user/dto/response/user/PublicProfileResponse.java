package media.social.modules.user.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicProfileResponse {

    private Long userId;

    private String username;

    private String avatarUrl;

    private String coverUrl;

    private String bio;

    private String fullName;

    private String website;

    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String country;

    private String city;

    private String district;

    private String occupation;

    private String company;

    private String education;

    private Map<String,String> socialLinks;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Long totalFriend;

    private boolean isFriend;
}
