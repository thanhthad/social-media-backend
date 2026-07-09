package media.social.modults.post.repository;

import media.social.modults.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    Optional<PostMedia> findByPublicId(String publicId);

    List<PostMedia> findByPostIdIn(List<Long> postIds);

    List<PostMedia> findByPostId(Long postId);

    long countByPostId(Long postId);

}