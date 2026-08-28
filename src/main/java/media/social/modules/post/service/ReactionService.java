package media.social.modules.post.service;

import media.social.modules.post.dto.response.post.PostReactionResponse;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ReactionService {

    PostReactionResponse react(
            Long postId,
            ReactionType type
    );

    PostReactionResponse loveReel(Long postId);

    ReactionCountResponse countReaction(
            Long postId
    );

    Page<UserReactionResponse> getUsersReacted(
            Long postId,
            ReactionType type,
            Pageable pageable
    );
}