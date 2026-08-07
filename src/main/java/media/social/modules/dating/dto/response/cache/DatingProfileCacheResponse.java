package media.social.modules.dating.dto.response.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class DatingProfileCacheResponse {

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