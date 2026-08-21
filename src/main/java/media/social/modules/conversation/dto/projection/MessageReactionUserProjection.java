package media.social.modules.conversation.dto.projection;

import media.social.modules.post.enums.ReactionType;

import java.time.OffsetDateTime;

public interface MessageReactionUserProjection {

    Long getUserId();

    String getUserName();

    String getAvatarUrl();

    ReactionType getReactionType();

    OffsetDateTime getCreatedAt();
}