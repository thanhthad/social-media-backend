package media.social.modults.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modults.conversation.enums.ConversationType;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ConversationResponse {

    private Long id;

    private ConversationType type;

    private OffsetDateTime createdAt;

}