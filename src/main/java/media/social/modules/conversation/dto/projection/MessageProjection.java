package media.social.modules.conversation.dto.projection;

import java.time.OffsetDateTime;

public interface MessageProjection {

    Long getId();

    Long getConversationId();

    Long getSenderId();

    String getSenderName();

    String getAvatarUrl();

    String getContent();

    Long getReplyToMessageId();

    OffsetDateTime getCreatedAt();

}