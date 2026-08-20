package media.social.modules.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.conversation.enums.ConversationType;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ConversationListResponse {

    private Long conversation_id;

    private Long user_id;

    private ConversationType type;

    private String displayName;

    private String avatarUrl;

    private String preview;

    private OffsetDateTime lastMessageAt;

    private Long unreadCount;

}