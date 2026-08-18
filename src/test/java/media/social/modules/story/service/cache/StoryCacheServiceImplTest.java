package media.social.modules.story.service.cache;

import media.social.modules.auth.Enum.Status;
import media.social.modules.story.dto.response.UserStoryResponse;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.story.service.cache.impl.StoryCacheServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
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
        List<UserStoryResponse> expectedResponse = Collections.emptyList();

        when(storyRepository.findActiveStoriesByUserId(eq(targetUserId), any(LocalDateTime.class), eq(Status.ACTIVE)))
                .thenReturn(expectedResponse);

        List<UserStoryResponse> result = storyCacheService.getUserStories(targetUserId);

        assertNotNull(result);
        assertSame(expectedResponse, result);
        verify(storyRepository).findActiveStoriesByUserId(eq(targetUserId), any(LocalDateTime.class), eq(Status.ACTIVE));
    }

    @Test
    void evictUserStories_doesNotThrow() {
        assertDoesNotThrow(() -> storyCacheService.evictUserStories(1L));
    }
}
