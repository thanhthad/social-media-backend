package media.social.modules.post.service.cache;

import media.social.modules.post.enums.ReactionType;

public interface ReactionCacheService {

    ReactionType getMyReaction(
            Long userId,
            Long postId
    );

    void evictMyReaction(
            Long userId,
            Long postId
    );
}