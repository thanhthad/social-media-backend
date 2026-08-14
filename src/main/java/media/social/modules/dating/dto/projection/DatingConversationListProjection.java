package media.social.modules.dating.dto.projection;

import java.time.OffsetDateTime;

public interface DatingConversationListProjection {

    Long getConversationId();

    Long getMatchId();

    Long getUserId();

    String getDisplayName();

    String getAvatarUrl();

    OffsetDateTime getMatchedAt();

    OffsetDateTime getLastMessageAt();
}