package media.social.modults.user.dto.request.block;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockRequest {
    @Positive(message = "BlockId must be positive")
    private Long blockedId;
}