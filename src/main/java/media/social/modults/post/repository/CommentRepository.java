package media.social.modults.post.repository;

import media.social.modults.post.dto.response.comment.CommentResponse;
import media.social.modults.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
    SELECT new media.social.modults.post.dto.response.comment.CommentResponse(
        c.id,
        u.id,
        u.username,
        p.avatarUrl,
        c.parent.id,
        c.content,
        c.createdAt,
    
        (SELECT COUNT(r)
            FROM Comment r
            WHERE r.parent.id = c.id),
    
        (SELECT COUNT(cr)
            FROM CommentReaction cr
            WHERE cr.comment.id = c.id),
    
        (
            SELECT cr.type
            FROM CommentReaction cr
            WHERE cr.comment.id = c.id
            AND cr.user.id = :userId
        )
    )
    FROM Comment c
    JOIN c.user u
    LEFT JOIN u.profile p
    WHERE c.post.id = :postId
    AND c.parent IS NULL
    ORDER BY c.createdAt DESC
    """)
    Page<CommentResponse> findRootComments(
            Long postId,
            Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT new media.social.modults.post.dto.response.comment.CommentResponse(
        c.id,
        u.id,
        u.username,
        p.avatarUrl,
        c.parent.id,
        c.content,
        c.createdAt,
    
        (SELECT COUNT(r)
            FROM Comment r
            WHERE r.parent.id = c.id),
    
        (SELECT COUNT(cr)
            FROM CommentReaction cr
            WHERE cr.comment.id = c.id),
    
        (
            SELECT cr.type
            FROM CommentReaction cr
            WHERE cr.comment.id = c.id
            AND cr.user.id = :userId
        )
    )
    FROM Comment c
    JOIN c.user u
    LEFT JOIN u.profile p
    WHERE c.parent.id = :parentId
    ORDER BY c.createdAt ASC
    """)
    Page<CommentResponse> findReplies(
            Long parentId,
            Long userId,
            Pageable pageable
    );

    Optional<Comment> findByIdAndUser_Id(Long commentId, Long userId);

    long countByParent_Id(Long parentId);
}