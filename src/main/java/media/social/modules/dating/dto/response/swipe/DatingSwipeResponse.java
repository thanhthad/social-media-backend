package media.social.modules.dating.dto.response.swipe;

import lombok.Builder;
import lombok.Data;
import media.social.modules.dating.enums.DatingSwipeAction;

import java.time.OffsetDateTime;

@Data
@Builder
public class DatingSwipeResponse {
    private Long targetUserId;
    private DatingSwipeAction action;
    private OffsetDateTime createdAt;
}