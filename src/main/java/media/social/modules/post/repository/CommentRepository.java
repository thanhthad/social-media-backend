package media.social.modules.post.repository;

import media.social.modules.post.dto.projection.CommentProjection;
import media.social.modules.post.dto.projection.RootCommentProjection;
import media.social.modules.post.dto.response.comment.CommentResponse;
import media.social.modules.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
    SELECT c
    FROM Comment c
    JOIN FETCH c.user
    JOIN FETCH c.post p
    JOIN FETCH p.user
    WHERE c.id = :commentId
    """)
    Optional<Comment> findByIdWithUserAndPost(Long commentId);

    @Query("""
    SELECT
        c.id AS commentId,
        u.id AS userId,
        u.username AS username,
        p.avatarUrl AS avatarUrl,
        c.parent.id AS parentId,
        c.content AS content,
        c.createdAt AS createdAt,

        COUNT(DISTINCT reply.id) AS totalReplies,
        COUNT(DISTINCT reaction.id) AS totalReactions,

        myReaction.type AS myReaction

    FROM Comment c

    JOIN c.user u

    LEFT JOIN u.profile p

    LEFT JOIN Comment reply
        ON reply.parent.id = c.id

    LEFT JOIN CommentReaction reaction
        ON reaction.comment.id = c.id

    LEFT JOIN CommentReaction myReaction
        ON myReaction.comment.id = c.id
        AND myReaction.user.id = :userId

    WHERE c.post.id = :postId
      AND c.parent IS NULL

    GROUP BY
        c.id,
        u.id,
        u.username,
        p.avatarUrl,
        c.parent.id,
        c.content,
        c.createdAt,
        myReaction.type

    ORDER BY c.createdAt DESC
""")
    Page<RootCommentProjection> findRootComments(
            @Param("postId") Long postId,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT
        c.id AS commentId,
        u.id AS userId,
        u.username AS username,
        p.avatarUrl AS avatarUrl,
        c.parent.id AS parentId,
        c.content AS content,
        c.createdAt AS createdAt,

        COUNT(DISTINCT reply.id) AS totalReplies,
        COUNT(DISTINCT reaction.id) AS totalReactions,

        myReaction.type AS myReaction

    FROM Comment c

    JOIN c.user u

    LEFT JOIN u.profile p

    LEFT JOIN Comment reply
        ON reply.parent.id = c.id

    LEFT JOIN CommentReaction reaction
        ON reaction.comment.id = c.id

    LEFT JOIN CommentReaction myReaction
        ON myReaction.comment.id = c.id
        AND myReaction.user.id = :userId

    WHERE c.parent.id = :parentId

    GROUP BY
        c.id,
        u.id,
        u.username,
        p.avatarUrl,
        c.parent.id,
        c.content,
        c.createdAt,
        myReaction.type

    ORDER BY c.createdAt ASC
""")
    Page<CommentProjection> findReplies(
            @Param("parentId") Long parentId,
            @Param("userId") Long userId,
            Pageable pageable
    );


    Optional<Comment> findByIdAndUser_Id(Long commentId, Long userId);

    @Modifying
    @Query("""
    UPDATE Comment c
    SET c.content = :content
    WHERE c.id = :commentId
    AND c.user.id = :userId
    """)
    int updateContent(
            Long commentId,
            Long userId,
            String content
    );

    long countByParent_Id(Long parentId);
}