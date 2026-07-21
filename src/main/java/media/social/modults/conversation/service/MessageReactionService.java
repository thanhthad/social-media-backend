package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.response.MessageReactionCountResponse;
import media.social.modults.conversation.dto.response.MessageReactionResponse;
import media.social.modults.conversation.dto.response.MessageReactionUserResponse;
import media.social.modults.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageReactionService {

    void react(
            Long messageId,
            ReactionType type
    );


    void removeReaction(
            Long messageId
    );

    MessageReactionResponse getMyReaction(
            Long messageId
    );

    MessageReactionCountResponse countReaction(
            Long messageId
    );

    Page<MessageReactionUserResponse> getUsersReacted(
            Long messageId,
            ReactionType type,
            Pageable pageable
    );

}