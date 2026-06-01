package media.social.modults.post.repository;

import media.social.modults.post.dto.response.PostFlatResponse;
import media.social.modults.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
SELECT new media.social.modults.post.dto.response.PostFlatResponse(
    p.id,
    p.content,
    pm.url,
    p.createdAt,
    u.id,
    u.username,
    pr.avatarUrl
)
FROM Post p
JOIN p.user u
LEFT JOIN Profile pr ON pr.user.id = u.id
LEFT JOIN PostMedia pm ON pm.post.id = p.id
WHERE u.id = :userId
ORDER BY p.createdAt DESC
""")
    Page<PostFlatResponse> findAllPostMe(
            @Param("userId") Long userId,
            Pageable pageable
    );

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