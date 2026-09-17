package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDatingLocationRequest {

    @Size(
            max = 100,
            message = "Country must be less than 100 characters"
    )
    private String country;

    @Size(
            max = 100,
            message = "City must be less than 100 characters"
    )
    private String city;

    @Size(
            max = 100,
            message = "District must be less than 100 characters"
    )
    private String district;
}