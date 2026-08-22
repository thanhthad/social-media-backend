package media.social.modules.story.service.cache;

import media.social.modules.auth.Enum.Status;
import media.social.modules.story.dto.projection.UserStoryProjection;
import media.social.modules.story.dto.response.UserStoryResponse;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.story.service.cache.impl.StoryCacheServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryCacheServiceImplTest {

    @InjectMocks
    private StoryCacheServiceImpl storyCacheService;

    @Mock
    private StoryRepository storyRepository;

    @Test
    void getUserStories_success() {
        Long targetUserId = 1L;
        UserStoryProjection projection = mock(UserStoryProjection.class);
        when(projection.getStoryId()).thenReturn(10L);
        when(projection.getUserId()).thenReturn(targetUserId);
        when(projection.getUsername()).thenReturn("username");
        when(projection.getContent()).thenReturn("content");

        when(storyRepository.findStoriesByUserId(targetUserId, Status.ACTIVE))
                .thenReturn(List.of(projection));

        List<UserStoryResponse> result = storyCacheService.getUserStories(targetUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getStoryId());
        assertEquals(targetUserId, result.get(0).getUserId());
        assertEquals("username", result.get(0).getUsername());
        assertEquals("content", result.get(0).getContent());
        verify(storyRepository).findStoriesByUserId(targetUserId, Status.ACTIVE);
    }

    @Test
    void evictUserStories_doesNotThrow() {
        assertDoesNotThrow(() -> storyCacheService.evictUserStories(1L));
    }
}
