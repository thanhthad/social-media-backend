package media.social.modules.user.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.auth.Enum.Status;

@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private Status status;
}