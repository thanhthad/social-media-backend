package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import media.social.modules.post.enums.Visibility;

@Data
public class UpdateDatingVisibilityRequest {

    @NotNull(message = "Visibility is required")
    private Visibility visibility;
}