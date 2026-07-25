package media.social.modults.post.repository;

import media.social.modults.post.dto.response.post.PostFlatResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
    SELECT p
    FROM Post p
    WHERE p.id = :postId
""")
    Optional<Post> findByIdWithUser(
            @Param("postId") Long postId
    );

    @Query("""
    SELECT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
    
        u.id,
        u.username,
        pr.avatarUrl,
    
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
    
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
    )
        FROM Post p
        JOIN p.user u
        LEFT JOIN u.profile pr
        WHERE u.id = :userId
            AND u.status = media.social.modults.user.Enum.Status.ACTIVE
            AND NOT EXISTS (
                SELECT 1
                FROM Report report
                WHERE report.post.id = p.id
                AND report.status = media.social.modults.post.enums.ReportStatus.APPROVED
            )
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
        pr.avatarUrl,
    
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
    
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
            
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr
    WHERE u.id = :userId
    AND p.visibility IN :visibilities
    AND u.status = media.social.modults.user.Enum.Status.ACTIVE
    AND NOT EXISTS (
        SELECT 1
        FROM Report r
        WHERE r.post.id = p.id
          AND r.status = media.social.modults.post.enums.ReportStatus.APPROVED
            )
    ORDER BY p.createdAt DESC
    """)
    Page<PostFlatResponse> findAllVisiblePost(
            @Param("userId") Long userId,
            @Param("visibilities") List<Visibility> visibilities,
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
        pr.avatarUrl,
            
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
        
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
          
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr
    WHERE p.id = :postId
    AND u.status = media.social.modults.user.Enum.Status.ACTIVE
    AND p.visibility IN :visibilities
    AND NOT EXISTS (
        SELECT 1
        FROM Report r
        WHERE r.post.id = p.id
          AND r.status = media.social.modults.post.enums.ReportStatus.APPROVED
    )
    """)
    Optional<PostFlatResponse> findPostDetailById(
            @Param("postId") Long postId,
            @Param("visibilities") List<Visibility> visibilities
    );


    @Query("""
    SELECT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl,
              
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
        
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr
    WHERE NOT EXISTS (
        SELECT 1
        FROM Block b
        WHERE (b.blocker.id = :viewerId AND b.blocked.id = u.id)
           OR (b.blocker.id = u.id AND b.blocked.id = :viewerId)
    )
   AND u.status = media.social.modults.user.Enum.Status.ACTIVE
   AND NOT EXISTS (
            SELECT 1
            FROM Report r
            WHERE r.post.id = p.id
              AND r.status = media.social.modults.post.enums.ReportStatus.APPROVED
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

    @Query("""
    SELECT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl,
            
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
        
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
    )
    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr
    WHERE NOT EXISTS(
        SELECT 1
        FROM Block b
        WHERE (b.blocker.id=:viewerId AND b.blocked.id=u.id)
           OR (b.blocker.id=u.id AND b.blocked.id=:viewerId)
    )
    AND u.status = media.social.modults.user.Enum.Status.ACTIVE
    AND NOT EXISTS (
            SELECT 1
            FROM Report r
            WHERE r.post.id = p.id
              AND r.status = media.social.modults.post.enums.ReportStatus.APPROVED
        )
    AND LOWER(p.content) LIKE LOWER(CONCAT('%',:keyword,'%'))
    AND (
            u.id=:viewerId
            OR p.visibility=media.social.modults.post.enums.Visibility.PUBLIC
            OR (
                p.visibility=media.social.modults.post.enums.Visibility.FOLLOWERS
                AND EXISTS(
                    SELECT 1
                    FROM Follow f
                    WHERE f.follower.id=:viewerId
                    AND f.following.id=u.id
                )
            )
    )
    ORDER BY p.createdAt DESC
    """)
    Page<PostFlatResponse> searchByContent(
            @Param("viewerId") Long viewerId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT new media.social.modults.post.dto.response.post.PostFlatResponse(
        p.id,
        p.content,
        p.visibility,
        p.createdAt,
        u.id,
        u.username,
        pr.avatarUrl,
            
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
        
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
    )
    FROM PostHashtag ph
    JOIN ph.post p
    JOIN p.user u
    LEFT JOIN u.profile pr
    WHERE LOWER(ph.hashtag.name)=LOWER(:name)
    AND u.status = media.social.modults.user.Enum.Status.ACTIVE
    AND NOT EXISTS(
        SELECT 1
        FROM Block b
        WHERE (b.blocker.id=:viewerId AND b.blocked.id=u.id)
           OR (b.blocker.id=u.id AND b.blocked.id=:viewerId)
    )
    AND NOT EXISTS (
            SELECT 1
            FROM Report r
            WHERE r.post.id = p.id
              AND r.status = media.social.modults.post.enums.ReportStatus.APPROVED
    )
    AND (
            u.id=:viewerId
            OR p.visibility=media.social.modults.post.enums.Visibility.PUBLIC
            OR (
                p.visibility=media.social.modults.post.enums.Visibility.FOLLOWERS
                AND EXISTS(
                    SELECT 1
                    FROM Follow f
                    WHERE f.follower.id=:viewerId
                    AND f.following.id=u.id
                )
            )
    )
    ORDER BY p.createdAt DESC
    """)
    Page<PostFlatResponse> searchByHashtag(
            @Param("viewerId") Long viewerId,
            @Param("name") String name,
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
        pr.avatarUrl,
            
        (SELECT COUNT(c.id)
         FROM Comment c
         WHERE c.post.id = p.id),
        
        (SELECT COUNT(r.id)
         FROM Reaction r
         WHERE r.post.id = p.id)
    )
    FROM SavedPost sp
    JOIN sp.post p
    JOIN p.user u
    LEFT JOIN u.profile pr
    WHERE sp.user.id = :userId
    AND u.status = media.social.modults.user.Enum.Status.ACTIVE
    AND NOT EXISTS (
        SELECT 1
        FROM Block b
        WHERE (b.blocker.id = :userId AND b.blocked.id = u.id)
           OR (b.blocker.id = u.id AND b.blocked.id = :userId)
    )
    AND NOT EXISTS (
        SELECT 1
        FROM Report r
        WHERE r.post.id = p.id
          AND r.status = media.social.modults.post.enums.ReportStatus.APPROVED
    )
    AND (
        u.id = :userId
        OR p.visibility = media.social.modults.post.enums.Visibility.PUBLIC
        OR (
            p.visibility = media.social.modults.post.enums.Visibility.FOLLOWERS
            AND EXISTS (
                SELECT 1
                FROM Follow f
                WHERE f.follower.id = :userId
                  AND f.following.id = u.id
            )
        )
    )
    ORDER BY sp.createdAt DESC
""")
    Page<PostFlatResponse> findSavedPosts(
            @Param("userId") Long userId,
            Pageable pageable
    );

}