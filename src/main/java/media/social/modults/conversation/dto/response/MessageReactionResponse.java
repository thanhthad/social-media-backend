package media.social.modults.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modults.post.enums.ReactionType;


@Getter
@Builder
public class MessageReactionResponse {

    private boolean reacted;

    private ReactionType type;

}