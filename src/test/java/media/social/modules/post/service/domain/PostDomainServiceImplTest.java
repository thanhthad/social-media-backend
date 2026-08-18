package media.social.modules.post.service.domain;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.entity.Post;
import media.social.modules.post.exception.post.ForbiddenException;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.service.domain.impl.PostDomainServiceImpl;
import media.social.modules.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostDomainServiceImplTest {

    @InjectMocks
    private PostDomainServiceImpl postDomainService;

    @Mock
    private PostRepository postRepository;

    @Test
    void validatePostExists_success() {
        Long postId = 1L;
        when(postRepository.existsById(postId)).thenReturn(true);

        assertDoesNotThrow(() -> postDomainService.validatePostExists(postId));
        verify(postRepository).existsById(postId);
    }

    @Test
    void validatePostExists_notFound_throwsPostNotFoundException() {
        Long postId = 1L;
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThrows(PostNotFoundException.class, () -> postDomainService.validatePostExists(postId));
    }

    @Test
    void getByPostId_success() {
        Long postId = 1L;
        Post post = new Post();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Post result = postDomainService.getByPostId(postId);

        assertNotNull(result);
        assertSame(post, result);
    }

    @Test
    void getByPostId_notFound_throwsPostNotFoundException() {
        Long postId = 1L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postDomainService.getByPostId(postId));
    }

    @Test
    void checkOwner_isOwner_doesNotThrow() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        Post post = Post.builder().user(user).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            assertDoesNotThrow(() -> postDomainService.checkOwner(post));
        }
    }

    @Test
    void checkOwner_notOwner_throwsForbiddenException() {
        Long userId = 1L;
        User user = new User();
        user.setId(2L);
        Post post = Post.builder().user(user).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            assertThrows(ForbiddenException.class, () -> postDomainService.checkOwner(post));
        }
    }

    @Test
    void increaseCommentCount_callsRepository() {
        Long postId = 1L;
        postDomainService.increaseCommentCount(postId);
        verify(postRepository).increaseCommentCount(postId);
    }

    @Test
    void decreaseCommentCount_callsRepository() {
        Long postId = 1L;
        postDomainService.decreaseCommentCount(postId);
        verify(postRepository).decreaseCommentCount(postId);
    }

    @Test
    void increaseReactionCount_callsRepository() {
        Long postId = 1L;
        postDomainService.increaseReactionCount(postId);
        verify(postRepository).increaseReactionCount(postId);
    }

    @Test
    void decreaseReactionCount_callsRepository() {
        Long postId = 1L;
        postDomainService.decreaseReactionCount(postId);
        verify(postRepository).decreaseReactionCount(postId);
    }
}
