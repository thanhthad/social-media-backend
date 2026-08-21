package media.social.modules.post.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.post.entity.Reaction;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.repository.ReactionRepository;
import media.social.modules.post.service.cache.ReactionCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionCacheServiceImpl
        implements ReactionCacheService {

    private final ReactionRepository reactionRepository;

    @Override
    @Cacheable(
            value = "user_reactions",
            key = "#userId + ':' + #postId"
    )
    public ReactionType getMyReaction(
            Long userId,
            Long postId
    ) {

        return reactionRepository
                .findByUserIdAndPostId(
                        userId,
                        postId
                )
                .map(Reaction::getType)
                .orElse(null);
    }

    @Override
    @CacheEvict(
            value = "user_reactions",
            key = "#userId + ':' + #postId"
    )
    public void evictMyReaction(
            Long userId,
            Long postId
    ) {
        // Cache invalidation handled by @CacheEvict.
    }
}