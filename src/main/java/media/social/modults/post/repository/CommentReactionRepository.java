package media.social.modults.post.repository;

import media.social.modults.post.dto.response.reaction.UserReactionResponse;
import media.social.modults.post.entity.CommentReaction;
import media.social.modults.post.enums.ReactionType;
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
        SELECT new media.social.modults.post.dto.response.reaction.UserReactionResponse(
            u.id,
            u.email,
            p.avatarUrl,
            r.createdAt
        )
        FROM CommentReaction r
        JOIN r.user u
        JOIN u.profile p
        WHERE r.comment.id = :commentId
        AND (:type IS NULL OR r.type = :type)
        """)
    Page<UserReactionResponse> findUsersReacted(
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