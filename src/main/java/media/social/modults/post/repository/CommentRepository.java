package media.social.modults.post.repository;

import media.social.modults.post.dto.response.CommentResponse;
import media.social.modults.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /*
     * ROOT COMMENTS
     */
    @Query("""
        SELECT new media.social.modults.post.dto.response.CommentResponse(
            c.id,
            u.id,
            u.username,
            p.avatarUrl,
            c.parent.id,
            c.content,
            c.createdAt,
            (SELECT COUNT(r) FROM Comment r WHERE r.parent.id = c.id)
        )
        FROM Comment c
        JOIN c.user u
        LEFT JOIN u.profile p
        WHERE c.post.id = :postId
          AND c.parent IS NULL
        ORDER BY c.createdAt DESC
    """)
    Page<CommentResponse> findRootComments(Long postId, Pageable pageable);


    /*
     * REPLIES
     */
    @Query("""
        SELECT new media.social.modults.post.dto.response.CommentResponse(
            c.id,
            u.id,
            u.username,
            p.avatarUrl,
            c.parent.id,
            c.content,
            c.createdAt,
            (SELECT COUNT(r) FROM Comment r WHERE r.parent.id = c.id)
        )
        FROM Comment c
        JOIN c.user u
        LEFT JOIN u.profile p
        WHERE c.parent.id = :parentId
        ORDER BY c.createdAt ASC
    """)
    Page<CommentResponse> findReplies(Long parentId, Pageable pageable);


    /*
     * COUNT ALL COMMENTS (post)
     */
    long countByPost_Id(Long postId);


    /*
     * FIND BY ID + OWNER (BEST PRACTICE)
     */
    Optional<Comment> findByIdAndUser_Id(Long commentId, Long userId);


    /*
     * COUNT DIRECT REPLIES
     */
    long countByParent_Id(Long parentId);
}