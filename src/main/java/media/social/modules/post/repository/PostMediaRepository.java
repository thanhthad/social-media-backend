package media.social.modules.post.repository;

import media.social.modules.post.dto.projection.ListPostMediaProjection;
import media.social.modules.post.dto.projection.PostMediaProjection;
import media.social.modules.post.dto.response.post.PostMediaResponse;
import media.social.modules.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    Optional<PostMedia> findByPublicId(String publicId);

    List<PostMedia> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    long countByPostId(Long postId);

    @Query("""
    SELECT
        pm.id AS postMediaId,
        pm.url AS url,
        pm.mediaType AS type
    FROM PostMedia pm
    WHERE pm.post.id = :postId
""")
    List<PostMediaProjection> findMediaByPostId(
            @Param("postId") Long postId
    );

    @Query("""
    SELECT
        pm.id AS postMediaId,
        pm.post.id AS postId,
        pm.url AS url,
        pm.mediaType AS type
    FROM PostMedia pm
    WHERE pm.post.id IN :postIds
""")
    List<ListPostMediaProjection> findMediaByPostIds(
            @Param("postIds") List<Long> postIds
    );

}