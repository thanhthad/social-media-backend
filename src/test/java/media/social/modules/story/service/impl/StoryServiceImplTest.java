package media.social.modules.story.service.impl;

import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.story.dto.projection.StoryFeedProjection;
import media.social.modules.story.dto.projection.StoryInteractionProjection;
import media.social.modules.story.dto.projection.UserStoryProjection;
import media.social.modules.story.dto.request.CreateStoryRequest;
import media.social.modules.story.dto.request.UpdateStoryVisibilityRequest;
import media.social.modules.story.dto.response.MyStoryResponse;
import media.social.modules.story.dto.response.StoryFeedResponse;
import media.social.modules.story.dto.response.UserStoryResponse;
import media.social.modules.story.entity.Story;
import media.social.modules.story.entity.StoryMedia;
import media.social.modules.story.exception.story.StoryForbiddenException;
import media.social.modules.story.exception.story.StoryNotFoundException;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.story.repository.StoryViewRepository;
import media.social.modules.story.service.cache.StoryCacheService;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceImplTest {

    @InjectMocks
    private StoryServiceImpl storyService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private MediaUploadService mediaUploadService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private FriendShipDomain friendShipDomain;

    @Mock
    private StoryCacheService storyCacheService;

    @Mock
    private StoryViewRepository storyViewRepository;

    @Test
    void create_withoutFile_savesStory() {
        Long userId = 1L;
        CreateStoryRequest request = new CreateStoryRequest();
        request.setContent("No file story");
        request.setVisibility(Visibility.PUBLIC);

        User user = new User();
        user.setId(userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);

            storyService.create(request);

            ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
            verify(storyRepository).save(captor.capture());
            Story saved = captor.getValue();
            assertNotNull(saved);
            assertEquals("No file story", saved.getContent());
            assertEquals(Visibility.PUBLIC, saved.getVisibility());
            assertNull(saved.getMedia());
            verify(storyCacheService).evictUserStories(userId);
            verifyNoInteractions(mediaUploadService);
        }
    }

    @Test
    void create_withFile_savesStoryAndUploadsFile() {
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        CreateStoryRequest request = new CreateStoryRequest();
        request.setContent("With file story");
        request.setVisibility(Visibility.FRIEND);
        request.setFile(file);

        User user = new User();
        user.setId(userId);

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("http://story-url.com")
                .publicId("public-id")
                .resourceType("IMAGE")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(mediaUploadService.upload(file, MediaUploadContext.STORY)).thenReturn(uploadResponse);

            storyService.create(request);

            ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
            verify(storyRepository).save(captor.capture());
            Story saved = captor.getValue();
            assertNotNull(saved);
            assertNotNull(saved.getMedia());
            assertEquals("http://story-url.com", saved.getMedia().getUrl());
            assertEquals("public-id", saved.getMedia().getPublicId());
            assertEquals(MediaType.IMAGE, saved.getMedia().getMediaType());
            verify(storyCacheService).evictUserStories(userId);
        }
    }

    @Test
    void getFeed_success() {
        Long viewerId = 1L;
        StoryFeedProjection projection = mock(StoryFeedProjection.class);
        when(projection.getUserId()).thenReturn(2L);
        when(projection.getContent()).thenReturn("Story content");
        List<StoryFeedProjection> expectedFeed = List.of(projection);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);
            when(storyRepository.findFeed(eq(viewerId), any(LocalDateTime.class), eq(FriendshipStatus.ACCEPTED),
                    eq(Visibility.PUBLIC), eq(Visibility.FRIEND), eq(Status.ACTIVE)))
                    .thenReturn(expectedFeed);

            List<StoryFeedResponse> result = storyService.getFeed();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getUserId());
            assertEquals("Story content", result.get(0).getContent());
        }
    }

    @Test
    void getUserStoriesBeforeExpire_success() {
        Long viewerId = 1L;
        Long targetUserId = 2L;
        UserStoryProjection projection = mock(UserStoryProjection.class);
        when(projection.getStoryId()).thenReturn(10L);
        when(projection.getUserId()).thenReturn(targetUserId);
        when(projection.getContent()).thenReturn("User story content");
        List<UserStoryProjection> expectedStories = List.of(projection);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);
            when(storyRepository.findUserStories(eq(viewerId), eq(targetUserId), any(LocalDateTime.class),
                    eq(FriendshipStatus.ACCEPTED), eq(Visibility.PUBLIC), eq(Visibility.FRIEND), eq(Status.ACTIVE)))
                    .thenReturn(expectedStories);

            List<UserStoryResponse> result = storyService.getUserStoriesBeforeExpire(targetUserId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getStoryId());
            assertEquals(targetUserId, result.get(0).getUserId());
            assertEquals("User story content", result.get(0).getContent());
        }
    }

    @Test
    void getUserStories_viewingOwnStories_returnsDirectly() {
        Long userId = 1L;
        List<UserStoryResponse> expectedStories = List.of(new UserStoryResponse());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyCacheService.getUserStories(userId)).thenReturn(expectedStories);

            List<UserStoryResponse> result = storyService.getUserStories(userId);

            assertNotNull(result);
            assertSame(expectedStories, result);
            verifyNoInteractions(friendShipDomain);
        }
    }

    @Test
    void getUserStories_viewingFriendStories_returnsPublicAndFriendVisibility() {
        Long viewerId = 1L;
        Long targetUserId = 2L;

        UserStoryResponse publicStory = UserStoryResponse.builder().storyId(10L).visibility(Visibility.PUBLIC).build();
        UserStoryResponse friendStory = UserStoryResponse.builder().storyId(11L).visibility(Visibility.FRIEND).build();
        UserStoryResponse privateStory = UserStoryResponse.builder().storyId(12L).visibility(Visibility.PRIVATE).build();

        List<UserStoryResponse> allStories = List.of(publicStory, friendStory, privateStory);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);
            when(storyCacheService.getUserStories(targetUserId)).thenReturn(allStories);
            when(friendShipDomain.areFriends(viewerId, targetUserId)).thenReturn(true);

            List<UserStoryResponse> result = storyService.getUserStories(targetUserId);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(publicStory));
            assertTrue(result.contains(friendStory));
            assertFalse(result.contains(privateStory));
        }
    }

    @Test
    void getUserStories_viewingNonFriendStories_returnsOnlyPublicVisibility() {
        Long viewerId = 1L;
        Long targetUserId = 2L;

        UserStoryResponse publicStory = UserStoryResponse.builder().storyId(10L).visibility(Visibility.PUBLIC).build();
        UserStoryResponse friendStory = UserStoryResponse.builder().storyId(11L).visibility(Visibility.FRIEND).build();
        UserStoryResponse privateStory = UserStoryResponse.builder().storyId(12L).visibility(Visibility.PRIVATE).build();

        List<UserStoryResponse> allStories = List.of(publicStory, friendStory, privateStory);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);
            when(storyCacheService.getUserStories(targetUserId)).thenReturn(allStories);
            when(friendShipDomain.areFriends(viewerId, targetUserId)).thenReturn(false);

            List<UserStoryResponse> result = storyService.getUserStories(targetUserId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.contains(publicStory));
            assertFalse(result.contains(friendStory));
            assertFalse(result.contains(privateStory));
        }
    }

    @Test
    void getMyStories_emptyStories_returnsEmptyList() {
        Long userId = 1L;
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyCacheService.getUserStories(userId)).thenReturn(Collections.emptyList());

            List<MyStoryResponse> result = storyService.getMyStories();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verifyNoInteractions(storyViewRepository);
        }
    }

    @Test
    void getMyStories_hasStories_returnsMappedStoriesWithInteractions() {
        Long userId = 1L;
        
        UserStoryResponse story1 = UserStoryResponse.builder()
                .storyId(10L)
                .userId(userId)
                .username("username")
                .content("content")
                .visibility(Visibility.PUBLIC)
                .build();
                
        List<UserStoryResponse> userStories = List.of(story1);

        StoryInteractionProjection projection = new StoryInteractionProjection() {
            @Override public Long getStoryId() { return 10L; }
            @Override public Long getUserId() { return 2L; }
            @Override public String getUsername() { return "viewer"; }
            @Override public String getAvatarUrl() { return "http://avatar"; }
            @Override public LocalDateTime getViewAt() { return null; }
            @Override public ReactionType getReactionType() { return ReactionType.LIKE; }
            @Override public LocalDateTime getReactionAt() { return null; }
        };

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyCacheService.getUserStories(userId)).thenReturn(userStories);
            when(storyViewRepository.findInteractionsByStoryIds(List.of(10L))).thenReturn(List.of(projection));

            List<MyStoryResponse> result = storyService.getMyStories();

            assertNotNull(result);
            assertEquals(1, result.size());
            MyStoryResponse myStory = result.get(0);
            assertEquals(10L, myStory.getStoryId());
            assertEquals(1, myStory.getInteractions().size());
            assertEquals("viewer", myStory.getInteractions().get(0).getUsername());
            assertEquals(ReactionType.LIKE, myStory.getInteractions().get(0).getReactionType());
        }
    }

    @Test
    void updateVisibility_success() {
        Long storyId = 10L;
        Long userId = 1L;
        
        UpdateStoryVisibilityRequest request = new UpdateStoryVisibilityRequest();
        request.setVisibility(Visibility.PRIVATE);

        User owner = new User();
        owner.setId(userId);
        
        Story story = new Story();
        story.setUser(owner);
        story.setVisibility(Visibility.PUBLIC);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.of(story));

            storyService.updateVisibility(storyId, request);

            assertEquals(Visibility.PRIVATE, story.getVisibility());
            verify(storyRepository).save(story);
            verify(storyCacheService).evictUserStories(userId);
        }
    }

    @Test
    void updateVisibility_notFound_throwsStoryNotFoundException() {
        Long storyId = 10L;
        UpdateStoryVisibilityRequest request = new UpdateStoryVisibilityRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(1L);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.empty());

            assertThrows(StoryNotFoundException.class, () -> storyService.updateVisibility(storyId, request));
        }
    }

    @Test
    void updateVisibility_notOwner_throwsStoryForbiddenException() {
        Long storyId = 10L;
        Long userId = 1L;
        UpdateStoryVisibilityRequest request = new UpdateStoryVisibilityRequest();

        User owner = new User();
        owner.setId(99L); // different owner
        
        Story story = new Story();
        story.setUser(owner);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.of(story));

            assertThrows(StoryForbiddenException.class, () -> storyService.updateVisibility(storyId, request));
            verify(storyRepository, never()).save(any());
        }
    }

    @Test
    void delete_successWithMedia() {
        Long storyId = 10L;
        Long userId = 1L;

        User owner = new User();
        owner.setId(userId);

        StoryMedia media = StoryMedia.builder()
                .publicId("public-id")
                .mediaType(MediaType.IMAGE)
                .build();

        Story story = new Story();
        story.setUser(owner);
        story.setMedia(media);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.of(story));

            storyService.delete(storyId);

            verify(mediaUploadService).delete("public-id", MediaType.IMAGE);
            verify(storyRepository).delete(story);
            verify(storyCacheService).evictUserStories(userId);
        }
    }

    @Test
    void delete_successWithoutMedia() {
        Long storyId = 10L;
        Long userId = 1L;

        User owner = new User();
        owner.setId(userId);

        Story story = new Story();
        story.setUser(owner);
        story.setMedia(null);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.of(story));

            storyService.delete(storyId);

            verifyNoInteractions(mediaUploadService);
            verify(storyRepository).delete(story);
            verify(storyCacheService).evictUserStories(userId);
        }
    }

    @Test
    void delete_notFound_throwsStoryNotFoundException() {
        Long storyId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(1L);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.empty());

            assertThrows(StoryNotFoundException.class, () -> storyService.delete(storyId));
        }
    }

    @Test
    void delete_notOwner_throwsStoryForbiddenException() {
        Long storyId = 10L;
        Long userId = 1L;

        User owner = new User();
        owner.setId(99L);

        Story story = new Story();
        story.setUser(owner);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(storyRepository.findByIdWithUserAndMedia(storyId)).thenReturn(Optional.of(story));

            assertThrows(StoryForbiddenException.class, () -> storyService.delete(storyId));
            verify(storyRepository, never()).delete(any(Story.class));
        }
    }
}
