package media.social.modules.dating.dto.response.conversation;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.conversation.dto.response.LastMessageResponse;

import java.time.OffsetDateTime;

@Getter
@Builder
public class DatingConversationListResponse {

    private Long conversationId;

    private Long matchId;

    private Long userId;

    private String displayName;

    private String avatarUrl;

    private LastMessageResponse lastMessage;

    private Long unreadCount;

    private OffsetDateTime matchedAt;

    private OffsetDateTime lastMessageAt;
}