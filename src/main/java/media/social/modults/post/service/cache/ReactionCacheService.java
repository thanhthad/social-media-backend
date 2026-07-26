package media.social.modults.post.service.cache;

import lombok.RequiredArgsConstructor;
import media.social.modults.post.entity.Reaction;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.post.repository.ReactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionCacheService {

    private final ReactionRepository reactionRepository;

    @Cacheable(
            value = "user_reactions",
            key = "#userId + ':' + #postId"
    )
    public ReactionType getMyReaction(
            Long userId,
            Long postId
    ){

        return reactionRepository
                .findByUserIdAndPostId(
                        userId,
                        postId
                )
                .map(Reaction::getType)
                .orElse(null);
    }

}