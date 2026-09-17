package media.social.modules.user.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUsernameRequest {

    @NotNull(message = "UserName is required")
    private String userName;
}
