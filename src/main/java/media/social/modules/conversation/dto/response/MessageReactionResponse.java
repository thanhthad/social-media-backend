package media.social.modules.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.post.enums.ReactionType;

import java.util.Map;

@Getter
@Builder
public class MessageReactionResponse {

    private Long messageId;

    private Long totalReactions;

    private Map<ReactionType, Long> counts;

    private ReactionType myReaction;

}