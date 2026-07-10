package media.social.modults.post.repository;

import media.social.modults.post.dto.response.post.PostDetailFlatResponse;
import media.social.modults.post.dto.response.post.PostFlatResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""

            SELECT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl
    )
FROM Post p
JOIN p.user u
LEFT JOIN Profile pr ON pr.user.id = u.id
WHERE u.id = :userId
ORDER BY p.createdAt DESC
""")
    Page<PostFlatResponse> findAllPostMe(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN Profile pr ON pr.user.id = u.id
    WHERE u.id = :userId
    AND p.visibility IN :visibilities
    ORDER BY p.createdAt DESC
    """)
    Page<PostFlatResponse> findAllVisiblePost(
            @Param("userId") Long userId,
            @Param("visibilities") List<Visibility> visibilities,
            Pageable pageable
    );

    @Query("""
    SELECT new media.social.modults.post.dto.response.post.PostDetailFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN Profile pr
    ON pr.user.id = u.id
    WHERE p.id = :postId
    """)
    Optional<PostDetailFlatResponse> findPostDetailById(Long postId);


    @Query("""
    SELECT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN Profile pr ON pr.user.id = u.id
    WHERE NOT EXISTS (
        SELECT 1
        FROM Block b
        WHERE (b.blocker.id = :viewerId AND b.blocked.id = u.id)
           OR (b.blocker.id = u.id AND b.blocked.id = :viewerId)
    )
    AND (
        u.id = :viewerId
        OR p.visibility = media.social.modults.post.enums.Visibility.PUBLIC
        OR (
            p.visibility = media.social.modults.post.enums.Visibility.FOLLOWERS
            AND EXISTS (
                SELECT 1
                FROM Follow f
                WHERE f.follower.id = :viewerId
                  AND f.following.id = u.id
            )
        )
    )
    ORDER BY p.createdAt DESC
    """)
    Page<PostFlatResponse> findFeed(
            @Param("viewerId") Long viewerId,
            Pageable pageable
    );

}