package media.social.modults.post.service;

import media.social.modults.post.dto.response.reaction.ReactionCountResponse;
import media.social.modults.post.dto.response.reaction.ReactionResponse;
import media.social.modults.post.dto.response.reaction.UserReactionResponse;
import media.social.modults.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ReactionService {

    void react(
            Long postId,
            ReactionType type
    );

    void removeReaction(
            Long postId
    );

    ReactionResponse getMyReaction(
            Long postId
    );

    long getTotalReaction(Long postId);

    ReactionCountResponse countReaction(
            Long postId
    );

    Page<UserReactionResponse> getUsersReacted(
            Long postId,
            ReactionType type,
            Pageable pageable
    );
}