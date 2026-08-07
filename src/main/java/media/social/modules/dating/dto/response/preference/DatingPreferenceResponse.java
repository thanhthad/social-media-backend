package media.social.modules.dating.dto.response.preference;

import lombok.Data;
import lombok.Builder;
import media.social.modules.dating.enums.GenderPreference;

import java.time.OffsetDateTime;

@Data
@Builder
public class DatingPreferenceResponse {

    private Integer minAge;

    private Integer maxAge;

    private GenderPreference genderPreference;

    private Integer maxDistance;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}