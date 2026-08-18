package media.social.modules.story.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.story.entity.Story;
import media.social.modules.story.entity.StoryView;
import media.social.modules.story.entity.StoryViewId;
import media.social.modules.story.repository.StoryViewRepository;
import media.social.modules.story.service.domain.StoryDomainService;
import media.social.modules.story.service.impl.StoryViewServiceImpl;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryViewServiceImplTest {

    @InjectMocks
    private StoryViewServiceImpl storyViewService;

    @Mock
    private StoryViewRepository storyViewRepository;

    @Mock
    private StoryDomainService storyDomainService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Test
    void view_newView_savesStoryView() {
        Long storyId = 1L;
        Long userId = 2L;

        Story story = new Story();
        User user = new User();
        user.setId(userId);
        
        StoryViewId expectedViewId = new StoryViewId(storyId, userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyDomainService.getActiveByStoryId(storyId)).thenReturn(story);
            doNothing().when(storyDomainService).checkCanViewStory(story, userId);
            when(storyViewRepository.existsById(expectedViewId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);

            storyViewService.view(storyId);

            ArgumentCaptor<StoryView> captor = ArgumentCaptor.forClass(StoryView.class);
            verify(storyViewRepository).save(captor.capture());
            StoryView saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(expectedViewId, saved.getId());
            assertEquals(story, saved.getStory());
            assertEquals(user, saved.getViewer());
        }
    }

    @Test
    void view_existingView_doesNotSave() {
        Long storyId = 1L;
        Long userId = 2L;

        Story story = new Story();
        StoryViewId expectedViewId = new StoryViewId(storyId, userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyDomainService.getActiveByStoryId(storyId)).thenReturn(story);
            doNothing().when(storyDomainService).checkCanViewStory(story, userId);
            when(storyViewRepository.existsById(expectedViewId)).thenReturn(true);

            storyViewService.view(storyId);

            verify(userServiceDomain, never()).getByUserId(any());
            verify(storyViewRepository, never()).save(any());
        }
    }

    @Test
    void view_storyNotFound_throwsStoryNotFoundException() {
        Long storyId = 999L;
        Long userId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(storyDomainService.getActiveByStoryId(storyId))
                    .thenThrow(new media.social.modules.story.exception.story.StoryNotFoundException("Story not found"));

            assertThrows(media.social.modules.story.exception.story.StoryNotFoundException.class,
                    () -> storyViewService.view(storyId));
            verify(storyViewRepository, never()).save(any());
        }
    }

    @Test
    void view_noPermission_throwsStoryAccessDeniedException() {
        Long storyId = 1L;
        Long userId = 2L;

        Story story = new Story();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(storyDomainService.getActiveByStoryId(storyId)).thenReturn(story);
            doThrow(new media.social.modules.story.exception.story.StoryAccessDeniedException("No permission"))
                    .when(storyDomainService).checkCanViewStory(story, userId);

            assertThrows(media.social.modules.story.exception.story.StoryAccessDeniedException.class,
                    () -> storyViewService.view(storyId));
            verify(storyViewRepository, never()).save(any());
        }
    }
}
