package media.social.modules.post.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.post.dto.response.hashtag.HashtagResponse;
import media.social.modules.post.dto.response.hashtag.TrendingHashtagResponse;
import media.social.modules.post.repository.HashtagRepository;
import media.social.modules.post.repository.PostHashtagRepository;
import media.social.modules.post.service.HashtagService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HashtagServiceImpl implements HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponse> search(String keyword) {

        return hashtagRepository
                .findByNameStartingWithIgnoreCase(keyword.trim())
                .stream()
                .map(hashtag -> HashtagResponse.builder()
                        .id(hashtag.getHashtagId())
                        .name(hashtag.getName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendingHashtagResponse> getTrending() {

        return postHashtagRepository
                .getTrendingHashtags(PageRequest.of(0, 5))
                .stream()
                .map(projection -> TrendingHashtagResponse.builder()
                        .id(projection.getId())
                        .name(projection.getName())
                        .totalPosts(projection.getTotalPosts())
                        .build())
                .toList();
    }

}