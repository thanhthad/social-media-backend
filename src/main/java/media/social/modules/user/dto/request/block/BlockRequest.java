package media.social.modules.user.dto.request.block;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class BlockRequest {
    @NotNull(message = "Target user id cannot be null")
    @Positive(message = "Target user id must be positive")
    private Long blockedId;
}