package media.social.modules.post.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.dto.projection.UserReactionProjection;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.Reaction;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.exception.reaction.ReactionNotFoundException;
import media.social.modules.post.repository.ReactionRepository;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.post.service.impl.ReactionServiceImpl;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceImplTest {

    @InjectMocks
    private ReactionServiceImpl reactionService;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private PostDomainService postDomainService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private NotificationService notificationService;

    @Test
    void react_newReaction_savesReactionAndIncreasesCountAndNotifies() {
        Long userId = 1L;
        Long postOwnerId = 2L;
        Long postId = 10L;
        ReactionType type = ReactionType.LIKE;

        User user = new User();
        user.setId(userId);

        User postOwner = new User();
        postOwner.setId(postOwnerId);

        Post post = Post.builder()
                .id(postId)
                .user(postOwner)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postDomainService.getByPostId(postId)).thenReturn(post);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(reactionRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

            reactionService.react(postId, type);

            ArgumentCaptor<Reaction> captor = ArgumentCaptor.forClass(Reaction.class);
            verify(reactionRepository).save(captor.capture());
            Reaction saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(user, saved.getUser());
            assertEquals(post, saved.getPost());
            assertEquals(type, saved.getType());

            verify(postDomainService).increaseReactionCount(postId);
            verify(notificationService).create(postOwner, user, EntityType.POST, postId, NotificationType.POST_REACTION);
        }
    }

    @Test
    void react_existingDifferentReaction_updatesType() {
        Long userId = 1L;
        Long postId = 10L;
        ReactionType newType = ReactionType.LOVE;

        User user = new User();
        user.setId(userId);

        Post post = Post.builder().id(postId).user(new User()).build();

        Reaction existingReaction = Reaction.builder()
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postDomainService.getByPostId(postId)).thenReturn(post);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(reactionRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existingReaction));

            reactionService.react(postId, newType);

            assertEquals(newType, existingReaction.getType());
            verify(reactionRepository).save(existingReaction);
            verify(postDomainService, never()).increaseReactionCount(any());
            verifyNoInteractions(notificationService);
        }
    }

    @Test
    void react_existingSameReaction_doesNotSave() {
        Long userId = 1L;
        Long postId = 10L;
        ReactionType sameType = ReactionType.LIKE;

        User user = new User();
        user.setId(userId);

        Post post = Post.builder().id(postId).user(new User()).build();

        Reaction existingReaction = Reaction.builder()
                .type(sameType)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postDomainService.getByPostId(postId)).thenReturn(post);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(reactionRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existingReaction));

            reactionService.react(postId, sameType);

            verify(reactionRepository, never()).save(any());
        }
    }

    @Test
    void countReaction_returnsCountMap() {
        Long postId = 10L;
        List<Object[]> results = new ArrayList<>();
        results.add(new Object[]{ReactionType.LIKE, 15L});
        results.add(new Object[]{ReactionType.LOVE, 7L});

        when(reactionRepository.countReactionTypesByPostId(postId)).thenReturn(results);

        ReactionCountResponse response = reactionService.countReaction(postId);

        assertNotNull(response);
        assertEquals(15L, response.getCounts().get(ReactionType.LIKE));
        assertEquals(7L, response.getCounts().get(ReactionType.LOVE));
        assertEquals(0L, response.getCounts().get(ReactionType.SAD));
        verify(postDomainService).getByPostId(postId);
    }

    @Test
    void getUsersReacted_returnsPage() {
        Long postId = 10L;
        ReactionType type = ReactionType.LIKE;
        Pageable pageable = PageRequest.of(0, 10);
        UserReactionProjection projection = mock(UserReactionProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getUserName()).thenReturn("testuser");
        when(projection.getAvatarUrl()).thenReturn("http://avatar.url");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        when(projection.getCreatedAt()).thenReturn(now);

        Page<UserReactionProjection> expectedPage = new PageImpl<>(List.of(projection));

        when(reactionRepository.findUsersReacted(postId, type, pageable)).thenReturn(expectedPage);

        Page<UserReactionResponse> result = reactionService.getUsersReacted(postId, type, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        assertEquals("http://avatar.url", result.getContent().get(0).getAvatarUrl());
        assertEquals(now, result.getContent().get(0).getCreatedAt());
        verify(postDomainService).getByPostId(postId);
    }
}
