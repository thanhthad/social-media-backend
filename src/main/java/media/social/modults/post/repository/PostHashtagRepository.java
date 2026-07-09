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

    /**
     * Kiểm tra một bài viết đã gắn hashtag hay chưa.
     */
    boolean existsByPost_IdAndHashtag_HashtagId(
            Long postId,
            Long hashtagId
    );

    /**
     * Lấy tất cả hashtag của một bài viết.
     */
    List<PostHashtag> findByPost_Id(Long postId);

    /**
     * Xóa toàn bộ hashtag của bài viết.
     * Dùng khi cập nhật bài viết.
     */
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
