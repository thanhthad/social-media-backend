package media.social.modules.conversation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import media.social.modules.post.enums.ReactionType;

import java.time.OffsetDateTime;


@Getter
@AllArgsConstructor
public class MessageReactionUserResponse {

    private Long userId;

    private String userName;

    private String avatarUrl;

    private ReactionType reactionType;

    private OffsetDateTime createdAt;

}