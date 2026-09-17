package media.social.modules.post.service.impl;

import media.social.modules.post.dto.projection.TrendingHashtagProjection;
import media.social.modules.post.dto.response.hashtag.HashtagResponse;
import media.social.modules.post.dto.response.hashtag.TrendingHashtagResponse;
import media.social.modules.post.entity.Hashtag;
import media.social.modules.post.repository.HashtagRepository;
import media.social.modules.post.repository.PostHashtagRepository;
import media.social.modules.post.service.impl.HashtagServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashtagServiceImplTest {

    @InjectMocks
    private HashtagServiceImpl hashtagService;

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private PostHashtagRepository postHashtagRepository;

    @Test
    void search_keyword_returnsMappedHashtagResponse() {
        String keyword = "  spring  ";
        Hashtag hashtag = Hashtag.builder()
                .hashtagId(1L)
                .name("spring")
                .build();

        when(hashtagRepository.findByNameStartingWithIgnoreCase("spring")).thenReturn(List.of(hashtag));

        List<HashtagResponse> result = hashtagService.search(keyword);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("spring", result.get(0).getName());
        verify(hashtagRepository).findByNameStartingWithIgnoreCase("spring");
    }

    @Test
    void getTrending_returnsTrendingHashtags() {
        TrendingHashtagProjection trending = mock(TrendingHashtagProjection.class);
        when(trending.getId()).thenReturn(1L);
        when(trending.getName()).thenReturn("java");
        when(trending.getTotalPosts()).thenReturn(100L);

        when(postHashtagRepository.getTrendingHashtags(PageRequest.of(0, 5))).thenReturn(List.of(trending));

        List<TrendingHashtagResponse> result = hashtagService.getTrending();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("java", result.get(0).getName());
        assertEquals(100L, result.get(0).getTotalPosts());
        verify(postHashtagRepository).getTrendingHashtags(PageRequest.of(0, 5));
    }

    @Test
    void search_emptyResult_returnsEmptyList() {
        String keyword = "nonexistent";

        when(hashtagRepository.findByNameStartingWithIgnoreCase("nonexistent")).thenReturn(Collections.emptyList());

        List<HashtagResponse> result = hashtagService.search(keyword);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(hashtagRepository).findByNameStartingWithIgnoreCase("nonexistent");
    }
}
