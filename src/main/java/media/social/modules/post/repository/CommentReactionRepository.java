package media.social.modules.post.repository;

import media.social.modules.post.dto.projection.UserReactionProjection;
import media.social.modules.post.entity.CommentReaction;
import media.social.modules.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentReactionRepository
        extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByUserIdAndCommentId(
            Long userId,
            Long commentId
    );

    boolean existsByUserIdAndCommentId(
            Long userId,
            Long commentId
    );

    void deleteByUserIdAndCommentId(
            Long userId,
            Long commentId
    );

    @Query("""
        SELECT r.type, COUNT(r)
        FROM CommentReaction r
        WHERE r.comment.id = :commentId
        GROUP BY r.type
        """)
    List<Object[]> countReactionsByCommentId(
            @Param("commentId") Long commentId
    );

    @Query("""
    SELECT
        u.id AS id,
        u.email AS email,
        p.avatarUrl AS avatarUrl,
        r.createdAt AS createdAt

    FROM CommentReaction r

    JOIN r.user u
    JOIN u.profile p

    WHERE r.comment.id = :commentId

      AND (:type IS NULL OR r.type = :type)
""")
    Page<UserReactionProjection> findUsersReacted(
            @Param("commentId") Long commentId,
            @Param("type") ReactionType type,
            Pageable pageable
    );

    long countByCommentId(Long commentId);

    @Query("""
    SELECT cr.type
    FROM CommentReaction cr
    WHERE cr.user.id = :userId
      AND cr.comment.id = :commentId
    """)
    ReactionType findReactionType(
            @Param("userId") Long userId,
            @Param("commentId") Long commentId
    );
}