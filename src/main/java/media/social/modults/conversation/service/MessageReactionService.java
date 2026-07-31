package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.response.MessageReactionUserResponse;
import media.social.modults.post.enums.ReactionType;

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