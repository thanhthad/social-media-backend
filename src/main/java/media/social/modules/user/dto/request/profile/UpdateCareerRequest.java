package media.social.modules.user.dto.request.profile;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCareerRequest {

    @Size(
            max = 100,
            message = "Occupation cannot exceed 100 characters"
    )
    private String occupation;

    @Size(
            max = 100,
            message = "Company cannot exceed 100 characters"
    )
    private String company;

    @Size(
            max = 150,
            message = "Education cannot exceed 150 characters"
    )
    private String education;
}