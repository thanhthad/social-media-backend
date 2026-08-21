package media.social.modules.post.service.cache;

import media.social.modules.post.entity.Reaction;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.repository.ReactionRepository;
import media.social.modules.post.service.cache.impl.ReactionCacheServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionCacheServiceTest {

    @InjectMocks
    private ReactionCacheServiceImpl reactionCacheService;

    @Mock
    private ReactionRepository reactionRepository;

    @Test
    void getMyReaction_found_returnsReactionType() {
        Long userId = 1L;
        Long postId = 2L;
        Reaction reaction = Reaction.builder()
                .type(ReactionType.LIKE)
                .build();

        when(reactionRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(reaction));

        ReactionType result = reactionCacheService.getMyReaction(userId, postId);

        assertNotNull(result);
        assertEquals(ReactionType.LIKE, result);
        verify(reactionRepository).findByUserIdAndPostId(userId, postId);
    }

    @Test
    void getMyReaction_notFound_returnsNull() {
        Long userId = 1L;
        Long postId = 2L;

        when(reactionRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        ReactionType result = reactionCacheService.getMyReaction(userId, postId);

        assertNull(result);
        verify(reactionRepository).findByUserIdAndPostId(userId, postId);
    }

    @Test
    void evictMyReaction_doesNotThrow() {
        assertDoesNotThrow(() -> reactionCacheService.evictMyReaction(1L, 2L));
    }
}
