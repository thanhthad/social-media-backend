package media.social.modules.post.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.ReactionResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.entity.Comment;
import media.social.modules.post.entity.CommentReaction;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.exception.comment.CanNotCommentYourself;
import media.social.modules.post.exception.comment.CommentNotFoundException;
import media.social.modules.post.exception.reaction.ReactionNotFoundException;
import media.social.modules.post.repository.CommentReactionRepository;
import media.social.modules.post.repository.CommentRepository;
import media.social.modules.post.service.impl.CommentReactionServiceImpl;
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
class CommentReactionServiceImplTest {

    @InjectMocks
    private CommentReactionServiceImpl commentReactionService;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private NotificationService notificationService;

    @Test
    void react_newReaction_savesAndSendsNotification() {
        Long userId = 1L;
        Long commentOwnerId = 2L;
        Long commentId = 10L;
        ReactionType type = ReactionType.LIKE;

        User user = new User();
        user.setId(userId);

        User commentOwner = new User();
        commentOwner.setId(commentOwnerId);

        Comment comment = Comment.builder()
                .id(commentId)
                .user(commentOwner)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.empty());

            commentReactionService.react(commentId, type);

            ArgumentCaptor<CommentReaction> captor = ArgumentCaptor.forClass(CommentReaction.class);
            verify(commentReactionRepository).save(captor.capture());
            CommentReaction saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(user, saved.getUser());
            assertEquals(comment, saved.getComment());
            assertEquals(type, saved.getType());

            verify(notificationService).create(commentOwner, user, EntityType.COMMENT, commentId, NotificationType.COMMENT_REACTION);
        }
    }

    @Test
    void react_existingDifferentReaction_updatesType() {
        Long userId = 1L;
        Long commentOwnerId = 2L;
        Long commentId = 10L;
        ReactionType newType = ReactionType.LOVE;

        User user = new User();
        user.setId(userId);

        User commentOwner = new User();
        commentOwner.setId(commentOwnerId);

        Comment comment = Comment.builder()
                .id(commentId)
                .user(commentOwner)
                .build();

        CommentReaction existingReaction = CommentReaction.builder()
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(existingReaction));

            commentReactionService.react(commentId, newType);

            assertEquals(newType, existingReaction.getType());
            verify(commentReactionRepository).save(existingReaction);
            verifyNoInteractions(notificationService);
        }
    }

    @Test
    void react_existingSameReaction_doesNotSave() {
        Long userId = 1L;
        Long commentOwnerId = 2L;
        Long commentId = 10L;
        ReactionType sameType = ReactionType.LIKE;

        User user = new User();
        user.setId(userId);

        User commentOwner = new User();
        commentOwner.setId(commentOwnerId);

        Comment comment = Comment.builder()
                .id(commentId)
                .user(commentOwner)
                .build();

        CommentReaction existingReaction = CommentReaction.builder()
                .type(sameType)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(existingReaction));

            commentReactionService.react(commentId, sameType);

            verify(commentReactionRepository, never()).save(any());
        }
    }

    @Test
    void react_commentYourself_throwsCanNotCommentYourself() {
        Long userId = 1L;
        Long commentId = 10L;

        User user = new User();
        user.setId(userId);

        Comment comment = Comment.builder()
                .id(commentId)
                .user(user)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);

            assertThrows(CanNotCommentYourself.class, () -> commentReactionService.react(commentId, ReactionType.LIKE));
            verify(commentReactionRepository, never()).save(any());
        }
    }

    @Test
    void react_commentNotFound_throwsCommentNotFoundException() {
        Long userId = 1L;
        Long commentId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            assertThrows(CommentNotFoundException.class, () -> commentReactionService.react(commentId, ReactionType.LIKE));
        }
    }

    @Test
    void removeReaction_success_deletesAndRemovesNotification() {
        Long userId = 1L;
        Long commentOwnerId = 2L;
        Long commentId = 10L;

        User commentOwner = new User();
        commentOwner.setId(commentOwnerId);

        Comment comment = Comment.builder()
                .id(commentId)
                .user(commentOwner)
                .build();

        CommentReaction reaction = new CommentReaction();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(reaction));

            commentReactionService.removeReaction(commentId);

            verify(commentReactionRepository).delete(reaction);
            verify(notificationService).delete(commentOwnerId, userId, EntityType.COMMENT, commentId, NotificationType.COMMENT_REACTION);
        }
    }

    @Test
    void removeReaction_reactionNotFound_throwsReactionNotFoundException() {
        Long userId = 1L;
        Long commentId = 10L;

        Comment comment = new Comment();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.empty());

            assertThrows(ReactionNotFoundException.class, () -> commentReactionService.removeReaction(commentId));
            verify(commentReactionRepository, never()).delete(any());
        }
    }

    @Test
    void getMyReaction_reacted_returnsTrueAndType() {
        Long userId = 1L;
        Long commentId = 10L;

        Comment comment = new Comment();
        CommentReaction reaction = CommentReaction.builder()
                .type(ReactionType.HAHA)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(reaction));

            ReactionResponse result = commentReactionService.getMyReaction(commentId);

            assertNotNull(result);
            assertTrue(result.isReacted());
            assertEquals(ReactionType.HAHA, result.getType());
        }
    }

    @Test
    void getMyReaction_notReacted_returnsFalseAndNull() {
        Long userId = 1L;
        Long commentId = 10L;

        Comment comment = new Comment();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.empty());

            ReactionResponse result = commentReactionService.getMyReaction(commentId);

            assertNotNull(result);
            assertFalse(result.isReacted());
            assertNull(result.getType());
        }
    }

    @Test
    void countReaction_returnsMapOfCounts() {
        Long commentId = 10L;
        Comment comment = new Comment();

        List<Object[]> results = new ArrayList<>();
        results.add(new Object[]{ReactionType.LIKE, 12L});
        results.add(new Object[]{ReactionType.WOW, 3L});

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentReactionRepository.countReactionsByCommentId(commentId)).thenReturn(results);

        ReactionCountResponse response = commentReactionService.countReaction(commentId);

        assertNotNull(response);
        assertEquals(12L, response.getCounts().get(ReactionType.LIKE));
        assertEquals(3L, response.getCounts().get(ReactionType.WOW));
        assertEquals(0L, response.getCounts().get(ReactionType.SAD));
    }

    @Test
    void getUsersReacted_returnsPage() {
        Long commentId = 10L;
        ReactionType type = ReactionType.LIKE;
        Pageable pageable = PageRequest.of(0, 10);
        Comment comment = new Comment();
        UserReactionResponse userReaction = mock(UserReactionResponse.class);
        Page<UserReactionResponse> expectedPage = new PageImpl<>(List.of(userReaction));

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentReactionRepository.findUsersReacted(commentId, type, pageable)).thenReturn(expectedPage);

        Page<UserReactionResponse> result = commentReactionService.getUsersReacted(commentId, type, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertSame(expectedPage, result);
    }
}
