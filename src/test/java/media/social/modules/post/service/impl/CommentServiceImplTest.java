package media.social.modules.post.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.dto.projection.CommentProjection;
import media.social.modules.post.dto.projection.RootCommentProjection;
import media.social.modules.post.dto.request.comment.CreateCommentRequest;
import media.social.modules.post.dto.request.comment.UpdateCommentContent;
import media.social.modules.post.dto.response.comment.CommentResponse;
import media.social.modules.post.entity.Comment;
import media.social.modules.post.entity.Post;
import media.social.modules.post.exception.comment.CommentNotFoundException;
import media.social.modules.post.repository.CommentReactionRepository;
import media.social.modules.post.repository.CommentRepository;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.post.service.impl.CommentServiceImpl;
import media.social.modules.user.entity.Profile;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostDomainService postDomainService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void createComment_rootComment_savesAndSendsPostNotification() {
        long userId = 1L;
        Long postOwnerId = 2L;
        Long postId = 10L;

        CreateCommentRequest request = new CreateCommentRequest();
        request.setPostId(postId);
        request.setContent("Root comment");

        User user = new User();
        user.setId(userId);
        user.setUsername("commenter");
        Profile profile = new Profile();
        profile.setAvatarUrl("http://avatar.url");
        user.setProfile(profile);

        User postOwner = new User();
        postOwner.setId(postOwnerId);

        Post post = Post.builder()
                .id(postId)
                .user(postOwner)
                .build();

        Comment savedComment = Comment.builder()
                .id(100L)
                .content("Root comment")
                .createdAt(LocalDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postDomainService.getByPostId(postId)).thenReturn(post);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

            CommentResponse response = commentService.createComment(request);

            assertNotNull(response);
            assertEquals(100L, response.getCommentId());
            assertEquals("Root comment", response.getContent());
            assertEquals("commenter", response.getUsername());
            assertEquals("http://avatar.url", response.getAvatarUrl());
            assertNull(response.getParentId());

            verify(postDomainService).increaseCommentCount(postId);
            verify(notificationService).create(postOwner, user, EntityType.POST, postId, NotificationType.POST_COMMENT);
        }
    }

    @Test
    void createComment_replyComment_savesAndSendsReplyNotification() {
        long userId = 1L;
        Long parentOwnerId = 3L;
        Long postId = 10L;
        Long parentId = 50L;

        CreateCommentRequest request = new CreateCommentRequest();
        request.setPostId(postId);
        request.setParentId(parentId);
        request.setContent("Reply comment");

        User user = new User();
        user.setId(userId);
        user.setUsername("replier");

        User parentOwner = new User();
        parentOwner.setId(parentOwnerId);

        Post post = Post.builder()
                .id(postId)
                .user(new User())
                .build();

        Comment parentComment = Comment.builder()
                .id(parentId)
                .user(parentOwner)
                .build();

        Comment savedComment = Comment.builder()
                .id(101L)
                .content("Reply comment")
                .createdAt(LocalDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postDomainService.getByPostId(postId)).thenReturn(post);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(commentRepository.findById(parentId)).thenReturn(Optional.of(parentComment));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

            CommentResponse response = commentService.createComment(request);

            assertNotNull(response);
            assertEquals(101L, response.getCommentId());
            assertEquals(parentId, response.getParentId());

            verify(postDomainService).increaseCommentCount(postId);
            verify(notificationService).create(parentOwner, user, EntityType.COMMENT, parentId, NotificationType.COMMENT_REPLY);
        }
    }

    @Test
    void createComment_parentNotFound_throwsCommentNotFoundException() {
        long userId = 1L;
        Long postId = 10L;
        Long parentId = 50L;

        CreateCommentRequest request = new CreateCommentRequest();
        request.setPostId(postId);
        request.setParentId(parentId);

        Post post = new Post();
        User user = new User();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postDomainService.getByPostId(postId)).thenReturn(post);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(commentRepository.findById(parentId)).thenReturn(Optional.empty());

            assertThrows(CommentNotFoundException.class, () -> commentService.createComment(request));
            verify(commentRepository, never()).save(any());
        }
    }

    @Test
    void deleteComment_byCommentOwner_success() {
        Long userId = 1L;
        Long commentId = 100L;
        Long postOwnerId = 2L;
        Long postId = 10L;

        User commentOwner = new User();
        commentOwner.setId(userId);

        User postOwner = new User();
        postOwner.setId(postOwnerId);

        Post post = Post.builder().id(postId).user(postOwner).build();
        Comment comment = Comment.builder().id(commentId).user(commentOwner).post(post).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findByIdWithUserAndPost(commentId)).thenReturn(Optional.of(comment));

            commentService.deleteComment(commentId);

            verify(notificationService).delete(postOwnerId, userId, EntityType.POST, postId, NotificationType.POST_COMMENT);
            verify(commentRepository).delete(comment);
            verify(postDomainService).decreaseCommentCount(postId);
        }
    }

    @Test
    void deleteComment_byPostOwner_success() {
        Long userId = 2L;
        Long commentOwnerId = 1L;
        Long commentId = 100L;
        Long postId = 10L;

        User commentOwner = new User();
        commentOwner.setId(commentOwnerId);

        User postOwner = new User();
        postOwner.setId(userId);

        Post post = Post.builder().id(postId).user(postOwner).build();
        Comment comment = Comment.builder().id(commentId).user(commentOwner).post(post).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findByIdWithUserAndPost(commentId)).thenReturn(Optional.of(comment));

            commentService.deleteComment(commentId);

            verify(notificationService).delete(userId, commentOwnerId, EntityType.POST, postId, NotificationType.POST_COMMENT);
            verify(commentRepository).delete(comment);
            verify(postDomainService).decreaseCommentCount(postId);
        }
    }

    @Test
    void deleteComment_notAllowed_throwsAccessDeniedException() {
        Long userId = 99L;
        Long commentOwnerId = 1L;
        Long postOwnerId = 2L;
        Long commentId = 100L;

        User commentOwner = new User();
        commentOwner.setId(commentOwnerId);

        User postOwner = new User();
        postOwner.setId(postOwnerId);

        Post post = Post.builder().id(10L).user(postOwner).build();
        Comment comment = Comment.builder().id(commentId).user(commentOwner).post(post).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findByIdWithUserAndPost(commentId)).thenReturn(Optional.of(comment));

            assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(commentId));
            verify(commentRepository, never()).delete(any());
        }
    }

    @Test
    void deleteComment_notFound_throwsCommentNotFoundException() {
        Long commentId = 100L;
        when(commentRepository.findByIdWithUserAndPost(commentId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(commentId));
    }

    @Test
    void updateComment_success() {
        Long userId = 1L;
        Long commentId = 100L;

        UpdateCommentContent request = new UpdateCommentContent();
        request.setContent("Updated content");

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.updateContent(commentId, userId, "Updated content")).thenReturn(1);

            CommentResponse result = commentService.updateComment(commentId, request);

            assertNotNull(result);
            assertEquals(commentId, result.getCommentId());
            assertEquals("Updated content", result.getContent());
        }
    }

    @Test
    void updateComment_notUpdated_throwsCommentNotFoundException() {
        Long userId = 1L;
        Long commentId = 100L;

        UpdateCommentContent request = new UpdateCommentContent();
        request.setContent("Updated content");

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.updateContent(commentId, userId, "Updated content")).thenReturn(0);

            assertThrows(CommentNotFoundException.class, () -> commentService.updateComment(commentId, request));
        }
    }

    @Test
    void getRootComments_returnsPage() {
        Long userId = 1L;
        Long postId = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        RootCommentProjection projection = mock(RootCommentProjection.class);
        when(projection.getCommentId()).thenReturn(100L);
        Page<RootCommentProjection> expectedPage = new PageImpl<>(List.of(projection));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findRootComments(postId, userId, pageable)).thenReturn(expectedPage);

            Page<CommentResponse> result = commentService.getRootComments(postId, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(100L, result.getContent().get(0).getCommentId());
        }
    }

    @Test
    void getReplies_returnsPage() {
        Long userId = 1L;
        Long parentId = 50L;
        Pageable pageable = PageRequest.of(0, 10);
        CommentProjection projection = mock(CommentProjection.class);
        when(projection.getCommentId()).thenReturn(101L);
        Page<CommentProjection> expectedPage = new PageImpl<>(List.of(projection));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(commentRepository.findReplies(parentId, userId, pageable)).thenReturn(expectedPage);

            Page<CommentResponse> result = commentService.getReplies(parentId, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(101L, result.getContent().get(0).getCommentId());
        }
    }
}
