package media.social.modules.post.service;

import media.social.modules.post.dto.response.hashtag.HashtagResponse;
import media.social.modules.post.dto.response.hashtag.TrendingHashtagResponse;

import java.util.List;

public interface HashtagService {

    List<HashtagResponse> search(String keyword);

    List<TrendingHashtagResponse> getTrending();

}