package media.social.modules.conversation.dto.projection;

import media.social.modules.conversation.enums.ConversationType;

import java.time.OffsetDateTime;

public interface DatingConversationListProjection {

    Long getConversationId();

    Long getDatingProfileId();

    ConversationType getType();

    String getDisplayName();

    String getAvatarUrl();

    String getPreview();

    OffsetDateTime getLastMessageAt();

}
