package media.social.modults.post.repository;

import media.social.modults.post.entity.Like;
import media.social.modults.user.dto.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    void deleteByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    @Query("""
        SELECT new media.social.modults.user.dto.response.common.UserResponse(
            u.id,
            u.username,
            u.email,
            p.avatarUrl,
            u.createdAt
        )
        FROM Like l
        JOIN l.user u
        LEFT JOIN u.profile p
        WHERE l.post.id = :postId
        ORDER BY l.createdAt DESC
        """)
    Page<UserResponse> findUsersLikedPost(
            @Param("postId") Long postId,
            Pageable pageable
    );

    Page<Like> findByUserId(Long userId, Pageable pageable);


}