package media.social.modults.user.repository;

import media.social.modults.user.dto.response.FollowUserResponse;
import media.social.modults.user.entity.Follow;
import media.social.modults.user.entity.FollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /*
    ============================================================
    Kiểm tra đã follow chưa

    SELECT EXISTS(
        SELECT 1
        FROM follows
        WHERE follower_id = ?
          AND following_id = ?
    )
    ============================================================
     */
    boolean existsByFollower_IdAndFollowing_Id(
            Long followerId,
            Long followingId
    );


    /*
    ============================================================
    Xóa follow

    DELETE
    FROM follows
    WHERE follower_id = ?
      AND following_id = ?
    ============================================================
     */
    long deleteByFollower_IdAndFollowing_Id(
            Long followerId,
            Long followingId
    );


    /*
    ============================================================
    Đếm số người mình follow

    SELECT COUNT(*)
    FROM follows
    WHERE follower_id = ?
    ============================================================
     */
    long countByFollower_Id(Long userId);


    /*
    ============================================================
    Đếm số người follow mình

    SELECT COUNT(*)
    FROM follows
    WHERE following_id = ?
    ============================================================
     */
    long countByFollowing_Id(Long userId);


    /*
    ============================================================
    Những người follow tôi

    SELECT
        u.user_id,
        u.username,
        p.avatar_url
    FROM follows f
    JOIN users u
        ON u.user_id = f.follower_id
    LEFT JOIN profiles p
        ON p.user_id = u.user_id
    WHERE f.following_id = :userId
    ORDER BY f.created_at DESC
    ============================================================
     */
    @Query("""
        SELECT new media.social.modults.user.dto.response.FollowUserResponse(
            u.id,
            u.username,
            u.profile.avatarUrl
        )
        FROM Follow f
        JOIN f.follower u
        WHERE f.following.id = :userId
        ORDER BY f.createdAt DESC
    """)
    Page<FollowUserResponse> getFollowers(
            @Param("userId") Long userId,
            Pageable pageable
    );


    /*
    ============================================================
    Những người tôi follow

    SELECT
        u.user_id,
        u.username,
        p.avatar_url
    FROM follows f
    JOIN users u
        ON u.user_id = f.following_id
    LEFT JOIN profiles p
        ON p.user_id = u.user_id
    WHERE f.follower_id = :userId
    ORDER BY f.created_at DESC
    ============================================================
     */
    @Query("""
        SELECT new media.social.modults.user.dto.response.FollowUserResponse(
            u.id,
            u.username,
            u.profile.avatarUrl
        )
        FROM Follow f
        JOIN f.following u
        WHERE f.follower.id = :userId
        ORDER BY f.createdAt DESC
    """)
    Page<FollowUserResponse> getFollowing(
            @Param("userId") Long userId,
            Pageable pageable
    );

}

