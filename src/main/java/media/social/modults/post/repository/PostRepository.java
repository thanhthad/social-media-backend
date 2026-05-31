package media.social.modults.post.repository;

import media.social.modults.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserId(Long userId, Pageable pageable);

    Page<Post> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    Page<Post> findByContentContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    void deleteByUserId(Long userId);
}