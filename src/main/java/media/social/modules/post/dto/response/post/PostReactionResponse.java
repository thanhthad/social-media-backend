package media.social.modules.post.dto.response.post;

import lombok.Getter;
import lombok.Setter;
import media.social.modules.post.enums.ReactionType;

@Getter
@Setter
public class PostReactionResponse {

    private Long postId;

    private boolean isReacted;

    private ReactionType reactionType;

    private Long reactionCount;
}
