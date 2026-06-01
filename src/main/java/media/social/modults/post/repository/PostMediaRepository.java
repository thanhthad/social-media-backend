package media.social.modults.post.repository;

import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
}
