package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDatingCareerRequest {

    @Size(max = 100, message = "Occupation must be less than 100 characters")
    private String occupation;

    @Size(max = 150, message = "Education must be less than 150 characters")
    private String education;
}