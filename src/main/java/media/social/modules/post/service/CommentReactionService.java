package media.social.modules.post.service;

import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.ReactionResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentReactionService {

    void react(
            Long commentId,
            ReactionType type
    );

    void removeReaction(Long commentId);

    ReactionResponse getMyReaction(Long commentId);

    ReactionCountResponse countReaction(Long commentId);

    Page<UserReactionResponse> getUsersReacted(
            Long commentId,
            ReactionType type,
            Pageable pageable
    );
}