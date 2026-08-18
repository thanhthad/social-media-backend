package media.social.modules.post.service.cache;

import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.dto.response.post.PostFlatResponse;
import media.social.modules.post.dto.response.post.PostMediaResponse;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.repository.PostMediaRepository;
import media.social.modules.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCacheServiceTest {

    @InjectMocks
    private PostCacheService postCacheService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMediaRepository postMediaRepository;

    @Test
    void getPost_success() {
        Long postId = 1L;
        List<Visibility> visibilities = List.of(Visibility.PUBLIC);
        LocalDateTime now = LocalDateTime.now();

        PostFlatResponse flat = PostFlatResponse.builder()
                .id(postId)
                .content("Post content")
                .visibility(Visibility.PUBLIC)
                .createdAt(now)
                .userId(10L)
                .username("john_doe")
                .avatarUrl("http://avatar.url")
                .commentCount(5L)
                .reactionCount(15L)
                .build();

        PostMediaResponse media = PostMediaResponse.builder()
                .postMediaId(100L)
                .url("http://media.url")
                .type(MediaType.IMAGE)
                .build();

        when(postRepository.findPostDetailById(postId, visibilities)).thenReturn(Optional.of(flat));
        when(postMediaRepository.findMediaResponseByPostId(postId)).thenReturn(List.of(media));

        PostCacheDTO result = postCacheService.getPost(postId, visibilities);

        assertNotNull(result);
        assertEquals(postId, result.getId());
        assertEquals("Post content", result.getContent());
        assertEquals(Visibility.PUBLIC, result.getVisibility());
        assertEquals(now, result.getCreatedAt());
        assertEquals(10L, result.getUserId());
        assertEquals("john_doe", result.getUsername());
        assertEquals("http://avatar.url", result.getAvatarUrl());
        assertEquals(5L, result.getCommentCount());
        assertEquals(15L, result.getReactionCount());
        assertEquals(1, result.getPostMediaResponses().size());
        assertEquals(100L, result.getPostMediaResponses().get(0).getPostMediaId());

        verify(postRepository).findPostDetailById(postId, visibilities);
        verify(postMediaRepository).findMediaResponseByPostId(postId);
    }

    @Test
    void getPost_notFound_throwsPostNotFoundException() {
        Long postId = 1L;
        List<Visibility> visibilities = List.of(Visibility.PUBLIC);

        when(postRepository.findPostDetailById(postId, visibilities)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postCacheService.getPost(postId, visibilities));
        verify(postMediaRepository, never()).findMediaResponseByPostId(any());
    }
}
