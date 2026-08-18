package media.social.modules.dating.dto.response.profile;

import lombok.Builder;
import lombok.Data;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class MyDatingProfileResponse {

    private String username;

    private String avatarUrl;

    private String coverUrl;

    private String displayName;

    private String bio;

    private Gender gender;

    private LocalDate birthday;

    private Integer height;

    private String occupation;

    private String education;

    private String country;

    private String city;

    private String district;

    private Boolean active;

    private Visibility visibility;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}