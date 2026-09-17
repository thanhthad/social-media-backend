package media.social.modules.post.repository;

import media.social.modules.post.dto.projection.TrendingHashtagProjection;
import media.social.modules.post.dto.response.hashtag.TrendingHashtagResponse;
import media.social.modules.post.entity.PostHashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostHashtagRepository
        extends JpaRepository<PostHashtag, Long> {
    List<PostHashtag> findByPost_Id(Long postId);

    @Query("""
    SELECT
        h.hashtagId AS id,
        h.name AS name,
        COUNT(ph) AS totalPosts
    FROM PostHashtag ph
    JOIN ph.hashtag h
    GROUP BY h.hashtagId, h.name
    ORDER BY COUNT(ph) DESC
""")
    List<TrendingHashtagProjection> getTrendingHashtags(Pageable pageable);

}
