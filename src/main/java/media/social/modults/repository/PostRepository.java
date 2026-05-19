package media.social.modults.repository;

import media.social.modults.entity.Post;
import media.social.modults.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUser(User user);

    List<Post> findByUserId(Long userId);

    List<Post> findAllByOrderByCreatedAtDesc();
}