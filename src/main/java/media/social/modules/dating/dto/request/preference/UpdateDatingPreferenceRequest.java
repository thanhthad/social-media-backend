package media.social.modules.dating.dto.request.preference;

import jakarta.validation.constraints.*;
import lombok.Data;
import media.social.modules.dating.enums.GenderPreference;

@Data
public class UpdateDatingPreferenceRequest {

    @NotNull(message = "Minimum age is required")
    @Min(value = 18, message = "Minimum age must be at least 18")
    @Max(value = 100, message = "Minimum age must be less than 100")
    private Integer minAge;

    @NotNull(message = "Maximum age is required")
    @Min(value = 18, message = "Maximum age must be at least 18")
    @Max(value = 100, message = "Maximum age must be less than 100")
    private Integer maxAge;

    @NotNull(message = "Gender preference is required")
    private GenderPreference genderPreference;

    @NotNull(message = "Maximum distance is required")
    @Min(value = 1, message = "Maximum distance must be greater than 0")
    @Max(value = 500, message = "Maximum distance cannot exceed 500 km")
    private Integer maxDistance;
}