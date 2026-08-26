package media.social.modules.post.service.cache;

import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.PostFlatProjection;
import media.social.modules.post.dto.projection.PostMediaProjection;
import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.repository.PostMediaRepository;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.service.cache.impl.PostCacheServiceImpl;
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
    private PostCacheServiceImpl postCacheService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMediaRepository postMediaRepository;

    @Test
    void getPost_success() {
        Long postId = 1L;
        List<Visibility> visibilities = List.of(Visibility.PUBLIC);
        LocalDateTime now = LocalDateTime.now();

        PostFlatProjection flat = mock(PostFlatProjection.class);
        when(flat.getId()).thenReturn(postId);
        when(flat.getContent()).thenReturn("Post content");
        when(flat.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(flat.getCreatedAt()).thenReturn(now);
        when(flat.getUserId()).thenReturn(10L);
        when(flat.getUsername()).thenReturn("john_doe");
        when(flat.getAvatarUrl()).thenReturn("http://avatar.url");
        when(flat.getCommentCount()).thenReturn(5L);
        when(flat.getReactionCount()).thenReturn(15L);

        PostMediaProjection media = mock(PostMediaProjection.class);
        when(media.getPostMediaId()).thenReturn(100L);
        when(media.getUrl()).thenReturn("http://media.url");
        when(media.getType()).thenReturn(MediaType.IMAGE);

        when(postRepository.findPostDetailById(postId, PostType.POST, Status.ACTIVE, visibilities, ReportStatus.APPROVED)).thenReturn(Optional.of(flat));
        when(postMediaRepository.findMediaByPostId(postId)).thenReturn(List.of(media));

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

        verify(postRepository).findPostDetailById(postId, PostType.POST, Status.ACTIVE, visibilities, ReportStatus.APPROVED);
        verify(postMediaRepository).findMediaByPostId(postId);
    }

    @Test
    void getPost_notFound_throwsPostNotFoundException() {
        Long postId = 1L;
        List<Visibility> visibilities = List.of(Visibility.PUBLIC);

        when(postRepository.findPostDetailById(postId, PostType.POST, Status.ACTIVE, visibilities, ReportStatus.APPROVED)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postCacheService.getPost(postId, visibilities));
        verify(postMediaRepository, never()).findMediaByPostId(any());
    }

    @Test
    void evictPost_doesNotThrow() {
        assertDoesNotThrow(() -> postCacheService.evictPost(1L));
    }
}
