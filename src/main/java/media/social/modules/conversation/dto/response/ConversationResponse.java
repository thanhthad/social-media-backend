package media.social.modules.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.conversation.enums.ConversationType;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class ConversationResponse {

    private Long id;

    private ConversationType type;

    private String displayName;

    private String avatarUrl;

    private List<ConversationMemberResponse> members;

    private OffsetDateTime createdAt;
}