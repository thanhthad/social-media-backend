package media.social.modules.dating.dto.response.profile;

import lombok.Builder;
import lombok.Data;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DatingProfileResponse {

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

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean active;

    private Visibility visibility;
}