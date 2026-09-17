package media.social.modules.story.service.domain;

import media.social.modules.post.enums.Visibility;
import media.social.modules.story.entity.Story;
import media.social.modules.story.exception.story.StoryAccessDeniedException;
import media.social.modules.story.exception.story.StoryNotFoundException;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.FriendShipDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryDomainServiceTest {

    @InjectMocks
    private StoryDomainService storyDomainService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private FriendShipDomain friendShipDomain;

    @Test
    void getByStoryId_success() {
        Long storyId = 1L;
        Story story = new Story();
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));

        Story result = storyDomainService.getByStoryId(storyId);

        assertNotNull(result);
        assertSame(story, result);
    }

    @Test
    void getByStoryId_notFound_throwsStoryNotFoundException() {
        Long storyId = 1L;
        when(storyRepository.findById(storyId)).thenReturn(Optional.empty());

        assertThrows(StoryNotFoundException.class, () -> storyDomainService.getByStoryId(storyId));
    }

    @Test
    void getActiveByStoryId_success() {
        Long storyId = 1L;
        Story story = new Story();
        when(storyRepository.findByIdAndExpiresAtAfter(eq(storyId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(story));

        Story result = storyDomainService.getActiveByStoryId(storyId);

        assertNotNull(result);
        assertSame(story, result);
    }

    @Test
    void getActiveByStoryId_notFoundOrExpired_throwsStoryNotFoundException() {
        Long storyId = 1L;
        when(storyRepository.findByIdAndExpiresAtAfter(eq(storyId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(StoryNotFoundException.class, () -> storyDomainService.getActiveByStoryId(storyId));
    }

    @Test
    void checkCanViewStory_expired_throwsStoryNotFoundException() {
        Story story = new Story();
        story.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        assertThrows(StoryNotFoundException.class, () -> storyDomainService.checkCanViewStory(story, 2L));
    }

    @Test
    void checkCanViewStory_owner_success() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);
        story.setExpiresAt(LocalDateTime.now().plusHours(1));

        assertDoesNotThrow(() -> storyDomainService.checkCanViewStory(story, 1L));
    }

    @Test
    void checkCanViewStory_public_success() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);
        story.setExpiresAt(LocalDateTime.now().plusHours(1));
        story.setVisibility(Visibility.PUBLIC);

        assertDoesNotThrow(() -> storyDomainService.checkCanViewStory(story, 2L));
    }

    @Test
    void checkCanViewStory_private_throwsStoryAccessDeniedException() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);
        story.setExpiresAt(LocalDateTime.now().plusHours(1));
        story.setVisibility(Visibility.PRIVATE);

        assertThrows(StoryAccessDeniedException.class, () -> storyDomainService.checkCanViewStory(story, 2L));
    }

    @Test
    void checkCanViewStory_friend_isFriend_success() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);
        story.setExpiresAt(LocalDateTime.now().plusHours(1));
        story.setVisibility(Visibility.FRIEND);

        when(friendShipDomain.areFriends(1L, 2L)).thenReturn(true);

        assertDoesNotThrow(() -> storyDomainService.checkCanViewStory(story, 2L));
    }

    @Test
    void checkCanViewStory_friend_isNotFriend_throwsStoryAccessDeniedException() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);
        story.setExpiresAt(LocalDateTime.now().plusHours(1));
        story.setVisibility(Visibility.FRIEND);

        when(friendShipDomain.areFriends(1L, 2L)).thenReturn(false);

        assertThrows(StoryAccessDeniedException.class, () -> storyDomainService.checkCanViewStory(story, 2L));
    }

    @Test
    void checkOwner_success() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);

        assertDoesNotThrow(() -> storyDomainService.checkOwner(story, 1L));
    }

    @Test
    void checkOwner_notOwner_throwsStoryAccessDeniedException() {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setUser(user);

        assertThrows(StoryAccessDeniedException.class, () -> storyDomainService.checkOwner(story, 2L));
    }

    @Test
    void checkCanViewStory_ownerButExpired_throwsStoryNotFoundException() {
        Long ownerId = 1L;
        User owner = new User();
        owner.setId(ownerId);

        Story story = new Story();
        story.setUser(owner);
        story.setExpiresAt(LocalDateTime.now().minusHours(1)); // expired
        story.setVisibility(Visibility.PUBLIC);

        // Even though caller is the owner, expiry check runs FIRST → StoryNotFoundException
        assertThrows(StoryNotFoundException.class,
                () -> storyDomainService.checkCanViewStory(story, ownerId));
    }
}
