package media.social.user.service;

import media.social.modules.post.dto.request.post.CreatePostRequest;
import media.social.modules.post.entity.Post;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.repository.*;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.post.service.impl.PostServiceImpl;
import media.social.modules.user.entity.User;
import media.social.modules.user.security.context.UserContextHolder;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private PostMediaRepository postMediaRepository;
    @Mock private UserServiceDomain userServiceDomain;
    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostDomainService postDomainService;

    @InjectMocks
    private PostServiceImpl postService;

    private User testUser;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).username("testuser").build();
    }

    @Test
    @DisplayName("createPost - Should save post and handle media/hashtags")
    void createPost_Success() {
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            CreatePostRequest request = CreatePostRequest.builder()
                    .content("Hello #world")
                    .visibility(Visibility.PUBLIC)
                    .files(Collections.emptyList())
                    .build();

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(testUser);

            postService.createPost(request);

            verify(postRepository, times(1)).save(any(Post.class));
            verify(hashtagRepository, atLeastOnce()).findByName(anyString());
        }
    }

    @Test
    @DisplayName("deleteByPostId - Should delete media and post")
    void deletePost_Success() {
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            Post post = Post.builder().id(100L).user(testUser).build();
            when(postRepository.findById(100L)).thenReturn(java.util.Optional.of(post));

            postService.deleteByPostId(100L);

            verify(postDomainService).checkOwner(post);
            verify(postRepository).delete(post);
            verify(postMediaRepository).deleteByPostId(100L);
        }
    }
}