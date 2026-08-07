package media.social.modules.dating.dto.response.profile;

import lombok.Builder;
import lombok.Data;
import media.social.modules.user.enums.Gender;

@Data
@Builder
public class PublicDatingProfileResponse {

    private Long userId;

    private String username;

    private String avatarUrl;

    private String displayName;

    private String bio;

    private Gender gender;

    private Integer age;

    private Integer height;

    private String occupation;

    private String education;

    private String city;
}