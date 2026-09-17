package media.social.modules.user.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.post.enums.Visibility;

@Getter
@Setter
public class UpdateProfileVisibilityRequest {

    @NotNull(
            message = "Visibility cannot be null"
    )
    private Visibility visibility;

}