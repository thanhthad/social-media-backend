package media.social.modults.post.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.post.dto.response.hashtag.HashtagResponse;
import media.social.modults.post.dto.response.hashtag.TrendingHashtagResponse;
import media.social.modults.post.entity.Hashtag;
import media.social.modults.post.repository.HashtagRepository;
import media.social.modults.post.repository.PostHashtagRepository;
import media.social.modults.post.service.HashtagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class HashtagServiceImpl implements HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Hashtag> findByName(String name) {

        return hashtagRepository.findByName(
                name.toLowerCase(Locale.ROOT).trim()
        );
    }

    @Override
    public Hashtag findOrCreate(String name) {

        String normalized = name.toLowerCase(Locale.ROOT).trim();

        return hashtagRepository.findByName(normalized)
                .orElseGet(() ->
                        hashtagRepository.save(
                                Hashtag.builder()
                                        .name(normalized)
                                        .build()
                        )
                );
    }

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

        return postHashtagRepository.getTrendingHashtags();
    }

}