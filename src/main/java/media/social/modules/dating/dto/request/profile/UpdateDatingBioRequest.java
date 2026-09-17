package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDatingBioRequest {

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;
}