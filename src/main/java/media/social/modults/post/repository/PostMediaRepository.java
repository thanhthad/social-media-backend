package media.social.modults.post.repository;

import media.social.modults.post.dto.response.post.PostMediaResponse;
import media.social.modults.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    Optional<PostMedia> findByPublicId(String publicId);

    List<PostMedia> findByPostIdIn(List<Long> postIds);

    List<PostMedia> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    long countByPostId(Long postId);

    @Query("""
SELECT new media.social.modults.post.dto.response.post.PostMediaResponse(
    pm.id,
    pm.url,
    pm.mediaType
)

FROM PostMedia pm

WHERE pm.post.id = :postId
""")
    List<PostMediaResponse> findMediaResponseByPostId(Long postId);

}