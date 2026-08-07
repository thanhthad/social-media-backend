package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDatingStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}