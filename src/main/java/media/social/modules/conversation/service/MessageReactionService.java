package media.social.modules.conversation.service;

import media.social.modules.conversation.dto.response.MessageReactionUserResponse;
import media.social.modules.post.enums.ReactionType;

import java.util.List;

public interface MessageReactionService {

    void react(
            Long messageId,
            ReactionType type
    );

    void removeReaction(
            Long messageId
    );

    List<MessageReactionUserResponse> getUsersReacted(
            Long messageId
    );


}