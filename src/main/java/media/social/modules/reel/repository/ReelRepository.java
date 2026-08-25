package media.social.modules.reel.repository;

import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.reel.dto.projection.ReelFlatProjection;
import media.social.modules.reel.entity.ReelDetail;
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
public interface ReelRepository extends JpaRepository<ReelDetail, Long> {

    Optional<ReelDetail> findByPostId(Long postId);

    // ─── INCREMENT COUNTERS ─────────────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE ReelDetail r
        SET r.viewCount = r.viewCount + 1
        WHERE r.reelId = :reelId
    """)
    int incrementViewCount(@Param("reelId") Long reelId);

    @Modifying
    @Query("""
        UPDATE ReelDetail r
        SET r.shareCount = r.shareCount + 1
        WHERE r.reelId = :reelId
    """)
    int incrementShareCount(@Param("reelId") Long reelId);

    // ─── FIND REEL DETAIL BY POST ID ───────────────────────────────────────────

    @Query("""
        SELECT
            p.id               AS id,
            p.content          AS content,
            p.visibility       AS visibility,
            p.createdAt        AS createdAt,

            u.id               AS userId,
            u.username         AS username,
            pr.avatarUrl       AS avatarUrl,

            p.commentCount     AS commentCount,
            p.reactionCount    AS reactionCount,

            rd.durationSeconds AS durationSeconds,
            rd.width           AS width,
            rd.height          AS height,
            rd.thumbnailUrl    AS thumbnailUrl,
            rd.viewCount       AS viewCount,
            rd.shareCount      AS shareCount

        FROM Post p
        JOIN p.user u
        LEFT JOIN u.profile pr
        JOIN ReelDetail rd ON rd.post.id = p.id

        WHERE p.id = :reelId
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
    Optional<ReelFlatProjection> findReelDetailById(
            @Param("reelId") Long reelId,
            @Param("postType") PostType postType,
            @Param("userStatus") Status userStatus,
            @Param("visibilities") List<Visibility> visibilities,
            @Param("reportStatus") ReportStatus reportStatus
    );

    // ─── MY REELS ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT
            p.id               AS id,
            p.content          AS content,
            p.visibility       AS visibility,
            p.createdAt        AS createdAt,

            u.id               AS userId,
            u.username         AS username,
            pr.avatarUrl       AS avatarUrl,

            p.commentCount     AS commentCount,
            p.reactionCount    AS reactionCount,

            rd.durationSeconds AS durationSeconds,
            rd.width           AS width,
            rd.height          AS height,
            rd.thumbnailUrl    AS thumbnailUrl,
            rd.viewCount       AS viewCount,
            rd.shareCount      AS shareCount

        FROM Post p
        JOIN p.user u
        LEFT JOIN u.profile pr
        JOIN ReelDetail rd ON rd.post.id = p.id

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
    Page<ReelFlatProjection> findAllReelMe(
            @Param("userId") Long userId,
            @Param("userStatus") Status userStatus,
            @Param("reportStatus") ReportStatus reportStatus,
            @Param("postType") PostType postType,
            Pageable pageable
    );

    // ─── USER REELS (viewable by another user) ─────────────────────────────────

    @Query("""
        SELECT
            p.id               AS id,
            p.content          AS content,
            p.visibility       AS visibility,
            p.createdAt        AS createdAt,

            u.id               AS userId,
            u.username         AS username,
            pr.avatarUrl       AS avatarUrl,

            p.commentCount     AS commentCount,
            p.reactionCount    AS reactionCount,

            rd.durationSeconds AS durationSeconds,
            rd.width           AS width,
            rd.height          AS height,
            rd.thumbnailUrl    AS thumbnailUrl,
            rd.viewCount       AS viewCount,
            rd.shareCount      AS shareCount

        FROM Post p
        JOIN p.user u
        LEFT JOIN u.profile pr
        JOIN ReelDetail rd ON rd.post.id = p.id

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
                AND r.status = :reportStatus
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
    Page<ReelFlatProjection> findAllVisibleReelsByUser(
            @Param("viewerId") Long viewerId,
            @Param("targetUserId") Long targetUserId,
            @Param("activeStatus") Status activeStatus,
            @Param("reportStatus") ReportStatus reportStatus,
            @Param("postType") PostType postType,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            Pageable pageable
    );

    // ─── REEL FEED (from friends + self) ──────────────────────────────────────

    @Query("""
        SELECT
            p.id               AS id,
            p.content          AS content,
            p.visibility       AS visibility,
            p.createdAt        AS createdAt,

            u.id               AS userId,
            u.username         AS username,
            pr.avatarUrl       AS avatarUrl,

            p.commentCount     AS commentCount,
            p.reactionCount    AS reactionCount,

            rd.durationSeconds AS durationSeconds,
            rd.width           AS width,
            rd.height          AS height,
            rd.thumbnailUrl    AS thumbnailUrl,
            rd.viewCount       AS viewCount,
            rd.shareCount      AS shareCount

        FROM Post p
        JOIN p.user u
        LEFT JOIN u.profile pr
        JOIN ReelDetail rd ON rd.post.id = p.id

        WHERE NOT EXISTS (
            SELECT 1
            FROM Block b
            WHERE (b.blocker.id = :viewerId AND b.blocked.id = u.id)
               OR (b.blocker.id = u.id AND b.blocked.id = :viewerId)
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
              OR p.visibility IN (:publicVisibility, :friendVisibility)
          )

        ORDER BY p.createdAt DESC
    """)
    Page<ReelFlatProjection> findReelFeed(
            @Param("viewerId") Long viewerId,
            @Param("userStatus") Status userStatus,
            @Param("postType") PostType postType,
            @Param("reportStatus") ReportStatus reportStatus,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            Pageable pageable
    );

    // ─── EXPLORE (public reels, not from friends) ──────────────────────────────

    @Query("""
        SELECT
            p.id               AS id,
            p.content          AS content,
            p.visibility       AS visibility,
            p.createdAt        AS createdAt,

            u.id               AS userId,
            u.username         AS username,
            pr.avatarUrl       AS avatarUrl,

            p.commentCount     AS commentCount,
            p.reactionCount    AS reactionCount,

            rd.durationSeconds AS durationSeconds,
            rd.width           AS width,
            rd.height          AS height,
            rd.thumbnailUrl    AS thumbnailUrl,
            rd.viewCount       AS viewCount,
            rd.shareCount      AS shareCount

        FROM Post p
        JOIN p.user u
        LEFT JOIN u.profile pr
        JOIN ReelDetail rd ON rd.post.id = p.id

        WHERE NOT EXISTS (
            SELECT 1
            FROM Block b
            WHERE (b.blocker.id = :viewerId AND b.blocked.id = u.id)
               OR (b.blocker.id = u.id AND b.blocked.id = :viewerId)
        )

          AND u.status = :activeStatus
          AND p.postType = :postType

          AND NOT EXISTS (
              SELECT 1
              FROM Report r
              WHERE r.post.id = p.id
                AND r.status = :approvedReportStatus
          )

          AND u.id <> :viewerId

          AND p.visibility = :publicVisibility

          AND NOT EXISTS (
              SELECT 1
              FROM Friendship f
              WHERE (
                  (f.userOne.id = :viewerId AND f.userTwo.id = u.id)
                  OR
                  (f.userOne.id = u.id AND f.userTwo.id = :viewerId)
              )
              AND f.status = :acceptedStatus
          )

        ORDER BY rd.viewCount DESC, p.createdAt DESC
    """)
    Page<ReelFlatProjection> findReelExplore(
            @Param("viewerId") Long viewerId,
            @Param("activeStatus") Status activeStatus,
            @Param("postType") PostType postType,
            @Param("approvedReportStatus") ReportStatus approvedReportStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            Pageable pageable
    );
}
