package media.social.modults.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationMemberResponse {

    private Long userId;
    private String username;
    private String avatarUrl;
}