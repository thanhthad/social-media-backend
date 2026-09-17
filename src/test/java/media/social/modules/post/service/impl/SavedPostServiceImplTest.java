package media.social.modules.post.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.SavedPost;
import media.social.modules.post.entity.SavedPostId;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.CannotSaveOwnPostException;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.exception.post.PostPrivateException;
import media.social.modules.post.exception.saved_post.PostFriendsOnlyException;
import media.social.modules.post.exception.saved_post.SavedPostAlreadyExistsException;
import media.social.modules.post.exception.saved_post.SavedPostNotFoundException;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.repository.SavedPostRepository;
import media.social.modules.post.service.impl.SavedPostServiceImpl;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedPostServiceImplTest {

    @InjectMocks
    private SavedPostServiceImpl savedPostService;

    @Mock
    private SavedPostRepository savedPostRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private PostRepository postRepository;

    @Mock
    private FriendShipDomain friendShipDomain;

    @Test
    void savePost_success_publicPost() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.PUBLIC)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(false);

            savedPostService.savePost(postId);

            ArgumentCaptor<SavedPost> captor = ArgumentCaptor.forClass(SavedPost.class);
            verify(savedPostRepository).save(captor.capture());
            SavedPost saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(new SavedPostId(userId, postId), saved.getId());
            assertEquals(user, saved.getUser());
            assertEquals(post, saved.getPost());
        }
    }

    @Test
    void savePost_success_friendPost_areFriends() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.FRIEND)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(friendShipDomain.areFriends(userId, ownerId)).thenReturn(true);
            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(false);

            savedPostService.savePost(postId);

            verify(savedPostRepository).save(any(SavedPost.class));
        }
    }

    @Test
    void savePost_postNotFound_throwsPostNotFoundException() {
        Long userId = 1L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> savedPostService.savePost(postId));
            verify(savedPostRepository, never()).save(any());
        }
    }

    @Test
    void savePost_ownPost_throwsCannotSaveOwnPostException() {
        Long userId = 1L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        Post post = Post.builder()
                .user(user)
                .visibility(Visibility.PUBLIC)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(CannotSaveOwnPostException.class, () -> savedPostService.savePost(postId));
            verify(savedPostRepository, never()).save(any());
        }
    }

    @Test
    void savePost_privatePost_throwsPostPrivateException() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.PRIVATE)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PostPrivateException.class, () -> savedPostService.savePost(postId));
            verify(savedPostRepository, never()).save(any());
        }
    }

    @Test
    void savePost_friendPost_notFriends_throwsPostFriendsOnlyException() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.FRIEND)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(friendShipDomain.areFriends(userId, ownerId)).thenReturn(false);

            assertThrows(PostFriendsOnlyException.class, () -> savedPostService.savePost(postId));
            verify(savedPostRepository, never()).save(any());
        }
    }

    @Test
    void savePost_alreadySaved_throwsSavedPostAlreadyExistsException() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        User user = new User();
        user.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.PUBLIC)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(true);

            assertThrows(SavedPostAlreadyExistsException.class, () -> savedPostService.savePost(postId));
            verify(savedPostRepository, never()).save(any());
        }
    }

    @Test
    void unsavePost_success() {
        Long userId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.existsById(postId)).thenReturn(true);
            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(true);

            savedPostService.unsavePost(postId);

            verify(savedPostRepository).deleteByUser_IdAndPost_Id(userId, postId);
        }
    }

    @Test
    void unsavePost_postNotFound_throwsPostNotFoundException() {
        Long userId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.existsById(postId)).thenReturn(false);

            assertThrows(PostNotFoundException.class, () -> savedPostService.unsavePost(postId));
            verify(savedPostRepository, never()).deleteByUser_IdAndPost_Id(any(), any());
        }
    }

    @Test
    void unsavePost_notSaved_throwsSavedPostNotFoundException() {
        Long userId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.existsById(postId)).thenReturn(true);
            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(false);

            assertThrows(SavedPostNotFoundException.class, () -> savedPostService.unsavePost(postId));
            verify(savedPostRepository, never()).deleteByUser_IdAndPost_Id(any(), any());
        }
    }

    @Test
    void isSaved_returnsTrue_whenExists() {
        Long userId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(true);

            boolean result = savedPostService.isSaved(postId);

            assertTrue(result);
        }
    }

    @Test
    void isSaved_returnsFalse_whenNotExists() {
        Long userId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(false);

            boolean result = savedPostService.isSaved(postId);

            assertFalse(result);
        }
    }
}
