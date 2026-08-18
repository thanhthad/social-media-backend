package media.social.modules.dating.dto.request.swipe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import media.social.modules.dating.enums.DatingSwipeAction;

@Data
public class CreateDatingSwipeRequest {
    @NotNull(message = "Target user ID is required")
    @Positive(message = "Target user ID must be positive")
    private Long targetUserId;

    @NotNull(message = "Swipe action is required")
    private DatingSwipeAction action;
}