package media.social.modules.story.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.story.entity.Story;
import media.social.modules.story.entity.StoryReaction;
import media.social.modules.story.exception.reaction.StoryReactionNotFoundException;
import media.social.modules.story.repository.StoryReactionRepository;
import media.social.modules.story.service.domain.StoryDomainService;
import media.social.modules.story.service.impl.StoryReactionServiceImpl;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryReactionServiceImplTest {

    @InjectMocks
    private StoryReactionServiceImpl storyReactionService;

    @Mock
    private StoryReactionRepository storyReactionRepository;

    @Mock
    private StoryDomainService storyDomainService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Test
    void react_newReaction_savesReaction() {
        Long storyId = 1L;
        Long userId = 2L;
        ReactionType type = ReactionType.LIKE;

        Story story = new Story();
        User user = new User();
        user.setId(userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyDomainService.getByStoryId(storyId)).thenReturn(story);
            doNothing().when(storyDomainService).checkCanViewStory(story, userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(storyReactionRepository.findByUserIdAndStoryId(userId, storyId)).thenReturn(Optional.empty());

            storyReactionService.react(storyId, type);

            ArgumentCaptor<StoryReaction> captor = ArgumentCaptor.forClass(StoryReaction.class);
            verify(storyReactionRepository).save(captor.capture());
            StoryReaction saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(story, saved.getStory());
            assertEquals(user, saved.getUser());
            assertEquals(type, saved.getType());
        }
    }

    @Test
    void react_existingDifferentReaction_updatesType() {
        Long storyId = 1L;
        Long userId = 2L;
        ReactionType newType = ReactionType.LOVE;

        Story story = new Story();
        User user = new User();
        user.setId(userId);

        StoryReaction existingReaction = new StoryReaction();
        existingReaction.setType(ReactionType.LIKE);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyDomainService.getByStoryId(storyId)).thenReturn(story);
            doNothing().when(storyDomainService).checkCanViewStory(story, userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(storyReactionRepository.findByUserIdAndStoryId(userId, storyId)).thenReturn(Optional.of(existingReaction));

            storyReactionService.react(storyId, newType);

            assertEquals(newType, existingReaction.getType());
            verify(storyReactionRepository).save(existingReaction);
        }
    }

    @Test
    void react_existingSameReaction_doesNotSave() {
        Long storyId = 1L;
        Long userId = 2L;
        ReactionType sameType = ReactionType.LIKE;

        Story story = new Story();
        User user = new User();
        user.setId(userId);

        StoryReaction existingReaction = new StoryReaction();
        existingReaction.setType(sameType);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyDomainService.getByStoryId(storyId)).thenReturn(story);
            doNothing().when(storyDomainService).checkCanViewStory(story, userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(storyReactionRepository.findByUserIdAndStoryId(userId, storyId)).thenReturn(Optional.of(existingReaction));

            storyReactionService.react(storyId, sameType);

            verify(storyReactionRepository, never()).save(any());
        }
    }

    @Test
    void removeReaction_success() {
        Long storyId = 1L;
        Long userId = 2L;

        StoryReaction existingReaction = new StoryReaction();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyReactionRepository.findByUserIdAndStoryId(userId, storyId)).thenReturn(Optional.of(existingReaction));

            storyReactionService.removeReaction(storyId);

            verify(storyDomainService).getByStoryId(storyId);
            verify(storyReactionRepository).delete(existingReaction);
        }
    }

    @Test
    void removeReaction_notFound_throwsStoryReactionNotFoundException() {
        Long storyId = 1L;
        Long userId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            
            when(storyReactionRepository.findByUserIdAndStoryId(userId, storyId)).thenReturn(Optional.empty());

            assertThrows(StoryReactionNotFoundException.class, () -> storyReactionService.removeReaction(storyId));
            verify(storyReactionRepository, never()).delete(any());
        }
    }

    @Test
    void countReaction_success() {
        Long storyId = 1L;
        
        List<Object[]> results = new ArrayList<>();
        results.add(new Object[]{ReactionType.LIKE, 10L});
        results.add(new Object[]{ReactionType.LOVE, 5L});

        when(storyReactionRepository.countReactionTypesByStoryId(storyId)).thenReturn(results);

        ReactionCountResponse response = storyReactionService.countReaction(storyId);

        assertNotNull(response);
        assertNotNull(response.getCounts());
        assertEquals(10L, response.getCounts().get(ReactionType.LIKE));
        assertEquals(5L, response.getCounts().get(ReactionType.LOVE));
        assertEquals(0L, response.getCounts().get(ReactionType.HAHA)); // Default to 0
        verify(storyDomainService).getByStoryId(storyId);
    }

    @Test
    void react_storyNotFound_throwsStoryNotFoundException() {
        Long userId = 1L;
        Long storyId = 999L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(storyDomainService.getByStoryId(storyId))
                    .thenThrow(new media.social.modules.story.exception.story.StoryNotFoundException("Story not found"));

            assertThrows(media.social.modules.story.exception.story.StoryNotFoundException.class,
                    () -> storyReactionService.react(storyId, ReactionType.LIKE));
            verify(storyReactionRepository, never()).save(any());
        }
    }
}
