package media.social.modules.post.repository;

import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.PostFlatProjection;
import media.social.modules.post.dto.response.post.PostFlatResponse;
import media.social.modules.post.entity.Post;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Post p
    SET p.commentCount = p.commentCount + 1
    WHERE p.id = :postId
    """)
    void increaseCommentCount(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Post p
    SET p.commentCount =
        CASE
            WHEN p.commentCount > 0
            THEN p.commentCount - 1
            ELSE 0
        END
    WHERE p.id = :postId
    """)
    void decreaseCommentCount(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Post p
    SET p.reactionCount = p.reactionCount + 1
    WHERE p.id = :postId
    """)
    void increaseReactionCount(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Post p
    SET p.reactionCount =
        CASE
            WHEN p.reactionCount > 0
            THEN p.reactionCount - 1
            ELSE 0
        END
    WHERE p.id = :postId
    """)
    void decreaseReactionCount(Long postId);

    @Query("""
    SELECT p
    FROM Post p
    JOIN FETCH p.user
    WHERE p.id = :postId
      AND p.postType = :postType
    """)
    Optional<Post> findByIdWithUser(
            @Param("postId") Long postId,
            @Param("postType") PostType postType
    );

    @Query("""
    SELECT
        p.id AS id,
        p.content AS content,
        p.visibility AS visibility,
        p.createdAt AS createdAt,

        u.id AS userId,
        u.username AS username,
        pr.avatarUrl AS avatarUrl,

        p.commentCount AS commentCount,
        p.reactionCount AS reactionCount

    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr

    WHERE u.id = :userId
      AND u.status = :userStatus

      AND p.postType = :postType

      AND NOT EXISTS (
          SELECT 1
          FROM Report report
          WHERE report.post.id = p.id
            AND report.status = :reportStatus
      )

    ORDER BY p.createdAt DESC
""")
    Page<PostFlatProjection> findAllPostMe(
            @Param("userId") Long userId,
            @Param("userStatus") Status userStatus,
            @Param("reportStatus") ReportStatus reportStatus,
            @Param("postType") PostType postType,
            Pageable pageable
    );

    @Query("""
    SELECT
        p.id AS id,
        p.content AS content,
        p.visibility AS visibility,
        p.createdAt AS createdAt,

        u.id AS userId,
        u.username AS username,
        pr.avatarUrl AS avatarUrl,

        p.commentCount AS commentCount,
        p.reactionCount AS reactionCount

    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr

    WHERE u.id = :targetUserId

      AND u.status = :activeStatus

      AND p.postType = :postType

      AND NOT EXISTS (
          SELECT 1
          FROM Block b
          WHERE (b.blocker.id = :viewerId AND b.blocked.id = u.id)
             OR (b.blocker.id = u.id AND b.blocked.id = :viewerId)
      )

      AND NOT EXISTS (
          SELECT 1
          FROM Report r
          WHERE r.post.id = p.id
            AND r.status = :approvedReportStatus
      )

      AND (
          u.id = :viewerId

          OR p.visibility = :publicVisibility

          OR (
              p.visibility = :friendVisibility

              AND EXISTS (
                  SELECT 1
                  FROM Friendship f
                  WHERE (
                      (f.userOne.id = :viewerId AND f.userTwo.id = :targetUserId)
                      OR
                      (f.userOne.id = :targetUserId AND f.userTwo.id = :viewerId)
                  )
                  AND f.status = :acceptedStatus
              )
          )
      )

    ORDER BY p.createdAt DESC
""")
    Page<PostFlatProjection> findAllVisiblePost(
            @Param("viewerId") Long viewerId,
            @Param("targetUserId") Long targetUserId,
            @Param("activeStatus") Status activeStatus,
            @Param("approvedReportStatus") ReportStatus approvedReportStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            @Param("postType") PostType postType,
            Pageable pageable
    );

    @Query("""
    SELECT
        p.id AS id,
        p.content AS content,
        p.visibility AS visibility,
        p.createdAt AS createdAt,

        u.id AS userId,
        u.username AS username,
        pr.avatarUrl AS avatarUrl,

        p.commentCount AS commentCount,
        p.reactionCount AS reactionCount

    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr

    WHERE p.id = :postId
      AND p.postType = :postType
      AND u.status = :userStatus
      AND p.visibility IN :visibilities

      AND NOT EXISTS (
          SELECT 1
          FROM Report r
          WHERE r.post.id = p.id
            AND r.status = :reportStatus
      )
""")
    Optional<PostFlatProjection> findPostDetailById(
            @Param("postId") Long postId,
            @Param("postType") PostType postType,
            @Param("userStatus") Status userStatus,
            @Param("visibilities") List<Visibility> visibilities,
            @Param("reportStatus") ReportStatus reportStatus
    );


    @Query("""
    SELECT
        p.id AS id,
        p.content AS content,
        p.visibility AS visibility,
        p.createdAt AS createdAt,

        u.id AS userId,
        u.username AS username,
        pr.avatarUrl AS avatarUrl,

        p.commentCount AS commentCount,
        p.reactionCount AS reactionCount

    FROM Post p
    JOIN p.user u
    LEFT JOIN u.profile pr

    WHERE NOT EXISTS (
        SELECT 1
        FROM Block b
        WHERE (
            b.blocker.id = :viewerId
            AND b.blocked.id = u.id
        )
        OR (
            b.blocker.id = u.id
            AND b.blocked.id = :viewerId
        )
    )

    AND u.status = :userStatus

    AND p.postType = :postType

    AND NOT EXISTS (
        SELECT 1
        FROM Report r
        WHERE r.post.id = p.id
          AND r.status = :reportStatus
    )

    AND (
        u.id = :viewerId

        OR EXISTS (
            SELECT 1
            FROM Friendship f
            WHERE (
                (f.userOne.id = :viewerId AND f.userTwo.id = u.id)
                OR
                (f.userOne.id = u.id AND f.userTwo.id = :viewerId)
            )
            AND f.status = :acceptedStatus
        )
    )

    AND (
        u.id = :viewerId
        OR p.visibility IN (
            :publicVisibility,
            :friendVisibility
        )
    )

    ORDER BY p.createdAt DESC
""")
    Page<PostFlatProjection> findFeed(
            @Param("viewerId") Long viewerId,
            @Param("userStatus") Status userStatus,
            @Param("postType") PostType postType,
            @Param("reportStatus") ReportStatus reportStatus,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            Pageable pageable
    );

    @Query(
            value = """
    SELECT
        p.post_id          AS id,
        p.content          AS content,
        p.visibility       AS visibility,
        p.created_at       AS createdAt,

        u.user_id          AS userId,
        u.username         AS username,
        pr.avatar_url      AS avatarUrl,

        p.comment_count    AS commentCount,
        p.reaction_count   AS reactionCount

    FROM posts p

    JOIN users u
        ON u.user_id = p.user_id

    LEFT JOIN profiles pr
        ON pr.user_id = u.user_id

    WHERE NOT EXISTS (
        SELECT 1
        FROM blocks b
        WHERE (b.blocker_id = :viewerId AND b.blocked_id = u.user_id)
           OR (b.blocker_id = u.user_id AND b.blocked_id = :viewerId)
    )

    AND u.status = :activeStatus

    AND p.post_type = :postType

    AND NOT EXISTS (
        SELECT 1
        FROM reports r
        WHERE r.post_id = p.post_id
          AND r.status = :approvedReportStatus
    )

    AND u.user_id <> :viewerId

    AND p.visibility = :publicVisibility

    AND NOT EXISTS (
        SELECT 1
        FROM friendships f
        WHERE (
            (f.user_one_id = :viewerId AND f.user_two_id = u.user_id)
            OR
            (f.user_one_id = u.user_id AND f.user_two_id = :viewerId)
        )
        AND f.status = :acceptedStatus
    )

    ORDER BY
        (
            (p.reaction_count * 2.0 + p.comment_count * 3.0)
            /
            POW(
                GREATEST(
                    EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 3600.0,
                    0
                ) + 2,
                1.3
            )
        ) DESC,
        p.created_at DESC
    """,

            countQuery = """
    SELECT COUNT(*)
    FROM posts p
    JOIN users u
        ON u.user_id = p.user_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM blocks b
        WHERE (b.blocker_id = :viewerId AND b.blocked_id = u.user_id)
           OR (b.blocker_id = u.user_id AND b.blocked_id = :viewerId)
    )
    AND u.status = :activeStatus
    AND p.post_type = :postType
    AND NOT EXISTS (
        SELECT 1
        FROM reports r
        WHERE r.post_id = p.post_id
          AND r.status = :approvedReportStatus
    )
    AND u.user_id <> :viewerId
    AND p.visibility = :publicVisibility
    AND NOT EXISTS (
        SELECT 1
        FROM friendships f
        WHERE (
            (f.user_one_id = :viewerId AND f.user_two_id = u.user_id)
            OR
            (f.user_one_id = u.user_id AND f.user_two_id = :viewerId)
        )
        AND f.status = :acceptedStatus
    )
    """,

            nativeQuery = true
    )
    Page<PostFlatProjection> findExplore(
            @Param("viewerId") Long viewerId,
            @Param("activeStatus") String activeStatus,
            @Param("postType") String postType,
            @Param("approvedReportStatus") String approvedReportStatus,
            @Param("publicVisibility") String publicVisibility,
            @Param("acceptedStatus") String acceptedStatus,
            Pageable pageable
    );

    @Query(
            value = """
    SELECT
        p.post_id AS id,
        p.content AS content,
        p.visibility AS visibility,
        p.created_at AS createdAt,

        u.user_id AS userId,
        u.username AS username,
        pr.avatar_url AS avatarUrl,

        p.comment_count AS commentCount,
        p.reaction_count AS reactionCount

    FROM posts p

    JOIN users u
        ON u.user_id = p.user_id

    LEFT JOIN profiles pr
        ON pr.user_id = u.user_id

    WHERE NOT EXISTS (
        SELECT 1
        FROM blocks b
        WHERE
            (b.blocker_id = :viewerId AND b.blocked_id = u.user_id)
            OR
            (b.blocker_id = u.user_id AND b.blocked_id = :viewerId)
    )

    AND u.status = :activeStatus

    AND p.post_type = :postType

    AND NOT EXISTS (
        SELECT 1
        FROM reports r
        WHERE r.post_id = p.post_id
          AND r.status = :approvedReportStatus
    )

    AND similarity(p.content, :keyword) > 0.2

    AND (
        u.user_id = :viewerId
        OR p.visibility = :publicVisibility
        OR (
            p.visibility = :friendVisibility
            AND EXISTS (
                SELECT 1
                FROM friendships f
                WHERE (
                    (f.user_one_id = :viewerId
                     AND f.user_two_id = u.user_id)
                    OR
                    (f.user_one_id = u.user_id
                     AND f.user_two_id = :viewerId)
                )
                AND f.status = :acceptedStatus
            )
        )
    )

    ORDER BY
        similarity(p.content, :keyword) DESC,
        p.created_at DESC
    """,

            countQuery = """
    SELECT COUNT(*)

    FROM posts p

    JOIN users u
        ON u.user_id = p.user_id

    WHERE NOT EXISTS (
        SELECT 1
        FROM blocks b
        WHERE
            (b.blocker_id = :viewerId AND b.blocked_id = u.user_id)
            OR
            (b.blocker_id = u.user_id AND b.blocked_id = :viewerId)
    )

    AND u.status = :activeStatus

    AND p.post_type = :postType

    AND NOT EXISTS (
        SELECT 1
        FROM reports r
        WHERE r.post_id = p.post_id
          AND r.status = :approvedReportStatus
    )

    AND similarity(p.content, :keyword) > 0.2

    AND (
        u.user_id = :viewerId

        OR p.visibility = :publicVisibility

        OR (
            p.visibility = :friendVisibility

            AND EXISTS (
                SELECT 1
                FROM friendships f
                WHERE (
                    (f.user_one_id = :viewerId
                     AND f.user_two_id = u.user_id)

                    OR

                    (f.user_one_id = u.user_id
                     AND f.user_two_id = :viewerId)
                )
                AND f.status = :acceptedStatus
            )
        )
    )
    """,

            nativeQuery = true
    )
    Page<PostFlatProjection> searchByContent(
            @Param("viewerId") Long viewerId,
            @Param("keyword") String keyword,
            @Param("activeStatus") String activeStatus,
            @Param("postType") String postType,
            @Param("approvedReportStatus") String approvedReportStatus,
            @Param("publicVisibility") String publicVisibility,
            @Param("friendVisibility") String friendVisibility,
            @Param("acceptedStatus") String acceptedStatus,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT
        p.id AS id,
        p.content AS content,
        p.visibility AS visibility,
        p.createdAt AS createdAt,

        u.id AS userId,
        u.username AS username,
        pr.avatarUrl AS avatarUrl,

        p.commentCount AS commentCount,
        p.reactionCount AS reactionCount

    FROM PostHashtag ph
    JOIN ph.post p
    JOIN p.user u
    LEFT JOIN u.profile pr

    WHERE LOWER(ph.hashtag.name) = LOWER(:name)

      AND u.status = :activeStatus

      AND p.postType = :postType

      AND NOT EXISTS (
          SELECT 1
          FROM Block b
          WHERE (b.blocker.id = :viewerId AND b.blocked.id = u.id)
             OR (b.blocker.id = u.id AND b.blocked.id = :viewerId)
      )

      AND NOT EXISTS (
          SELECT 1
          FROM Report r
          WHERE r.post.id = p.id
            AND r.status = :approvedReportStatus
      )

      AND (
          u.id = :viewerId

          OR p.visibility = :publicVisibility

          OR (
              p.visibility = :friendVisibility

              AND EXISTS (
                  SELECT 1
                  FROM Friendship f
                  WHERE (
                      (f.userOne.id = :viewerId AND f.userTwo.id = u.id)
                      OR
                      (f.userOne.id = u.id AND f.userTwo.id = :viewerId)
                  )
                  AND f.status = :acceptedStatus
              )
          )
      )

    ORDER BY p.createdAt DESC
""")
    Page<PostFlatProjection> searchByHashtag(
            @Param("viewerId") Long viewerId,
            @Param("name") String name,
            @Param("activeStatus") Status activeStatus,
            @Param("postType") PostType postType,
            @Param("approvedReportStatus") ReportStatus approvedReportStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            Pageable pageable
    );

    @Query("""
        SELECT
            p.id AS id,
            p.content AS content,
            p.visibility AS visibility,
            p.createdAt AS createdAt,
    
            u.id AS userId,
            u.username AS username,
            pr.avatarUrl AS avatarUrl,
    
            p.commentCount AS commentCount,
            p.reactionCount AS reactionCount
    
        FROM SavedPost sp
        JOIN sp.post p
        JOIN p.user u
        LEFT JOIN u.profile pr
    
        WHERE sp.user.id = :userId
    
          AND u.status = :activeStatus
    
          AND p.postType = :postType
    
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
                AND r.status = :approvedReportStatus
          )
    
          AND (
              u.id = :userId
    
              OR p.visibility = :publicVisibility
    
              OR (
                  p.visibility = :friendVisibility
    
                  AND EXISTS (
                      SELECT 1
                      FROM Friendship f
                      WHERE (
                          (f.userOne.id = :userId AND f.userTwo.id = u.id)
                          OR
                          (f.userOne.id = u.id AND f.userTwo.id = :userId)
                      )
                      AND f.status = :acceptedStatus
                  )
              )
          )
    
        ORDER BY sp.createdAt DESC
    """)
    Page<PostFlatProjection> findSavedPosts(
            @Param("userId") Long userId,
            @Param("activeStatus") Status activeStatus,
            @Param("postType") PostType postType,
            @Param("approvedReportStatus") ReportStatus approvedReportStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            Pageable pageable
    );
}