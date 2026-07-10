package media.social.modults.post.repository;

import media.social.modults.post.dto.response.hashtag.TrendingHashtagResponse;
import media.social.modults.post.entity.PostHashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostHashtagRepository
        extends JpaRepository<PostHashtag, Long> {
    boolean existsByPost_IdAndHashtag_HashtagId(
            Long postId,
            Long hashtagId
    );

    long countByHashtag_HashtagId(Long hashtagId);

    List<PostHashtag> findByPost_Id(Long postId);

    void deleteByPost_Id(Long postId);

    @Query("""
        SELECT new media.social.modults.post.dto.response.TrendingHashtagResponse(
            h.hashtagId,
            h.name,
            COUNT(ph)
        )
        FROM PostHashtag ph
        JOIN ph.hashtag h
        GROUP BY h.hashtagId,h.name
        ORDER BY COUNT(ph) DESC
    """)
    List<TrendingHashtagResponse> getTrendingHashtags();

}
