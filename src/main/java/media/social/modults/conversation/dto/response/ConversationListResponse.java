package media.social.modults.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modults.conversation.enums.ConversationType;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ConversationListResponse {

    private Long id;

    private ConversationType type;

    private String displayName;

    private String avatarUrl;

    private LastMessageResponse lastMessage;

    private OffsetDateTime createdAt;

}