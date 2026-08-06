package media.social.modules.user.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private Long id;

    private String fullName;

    private String avatarUrl;

    private String coverUrl;

    private String bio;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String phone;

    private String website;

    private String country;

    private String city;

    private String district;

    private String occupation;

    private String company;

    private String education;

    private Map<String, String> socialLinks;

    private Visibility profileVisibility;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}