package media.social.modults.post.service;

import media.social.modults.post.dto.response.hashtag.HashtagResponse;
import media.social.modults.post.dto.response.hashtag.TrendingHashtagResponse;
import media.social.modults.post.entity.Hashtag;

import java.util.List;
import java.util.Optional;

public interface HashtagService {

    Optional<Hashtag> findByName(String name);

    Hashtag findOrCreate(String name);

    List<HashtagResponse> search(String keyword);

    List<TrendingHashtagResponse> getTrending();

}