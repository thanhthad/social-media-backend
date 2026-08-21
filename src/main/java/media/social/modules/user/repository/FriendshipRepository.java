package media.social.modules.user.repository;

import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.projection.FriendshipUserProjection;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByUserOne_IdAndUserTwo_Id(
            Long userOneId,
            Long userTwoId
    );

    @Query("""
    SELECT COUNT(f) > 0
    FROM Friendship f
    WHERE (
        (f.userOne.id = :userId AND f.userTwo.id = :targetUserId)
        OR
        (f.userOne.id = :targetUserId AND f.userTwo.id = :userId)
    )
    AND f.status = :status
    """)
    boolean areFriends(
            @Param("userId") Long userId,
            @Param("targetUserId") Long targetUserId,
            @Param("status") FriendshipStatus status
    );

    @Query("""
    SELECT
        u.id AS userId,
        u.username AS username,
        u.profile.avatarUrl AS avatarUrl

    FROM Friendship f

    JOIN User u
        ON (
            u.id = f.userOne.id
            OR u.id = f.userTwo.id
        )
        AND u.id != :userId

    WHERE (
        f.userOne.id = :userId
        OR f.userTwo.id = :userId
    )

    AND f.status = :status

    ORDER BY f.createdAt DESC
""")
    Page<FriendshipUserProjection> getMyFriends(
            @Param("userId") Long userId,
            @Param("status") FriendshipStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT
            u.id AS userId,
            u.username AS username,
            u.profile.avatarUrl AS avatarUrl
    
        FROM Friendship f
        JOIN User u
            ON (
                u.id = f.userOne.id
                OR u.id = f.userTwo.id
            )
            AND u.id != :userId
        WHERE (
            f.userOne.id = :userId
            OR f.userTwo.id = :userId
        )
        AND f.status = :status
    
        AND (
            f.visibility = :publicVisibility
    
            OR (
                f.visibility = :friendVisibility
                AND EXISTS (
                    SELECT 1
                    FROM Friendship viewerFriendship
                    WHERE (
                        (
                            viewerFriendship.userOne.id = :viewerId
                            AND viewerFriendship.userTwo.id = :userId
                        )
                        OR
                        (
                            viewerFriendship.userOne.id = :userId
                            AND viewerFriendship.userTwo.id = :viewerId
                        )
                    )
                    AND viewerFriendship.status = :acceptedStatus
                )
            )
        )
        ORDER BY f.createdAt DESC
    """)
    Page<FriendshipUserProjection> getFriends(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId,
            @Param("status") FriendshipStatus status,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            Pageable pageable
    );


    @Query(value = """
    WITH friend_pairs AS (
        SELECT
            user_one_id AS user_id,
            user_two_id AS friend_id
        FROM friendships
        WHERE status = 'ACCEPTED'

        UNION ALL

        SELECT
            user_two_id AS user_id,
            user_one_id AS friend_id
        FROM friendships
        WHERE status = 'ACCEPTED'
    )

    SELECT
        f2.friend_id AS suggested_user_id,
        u.username,
        p.avatar_url,
        COUNT(*) AS mutual_count

    FROM friend_pairs f1

    JOIN friend_pairs f2
        ON f1.friend_id = f2.user_id

    JOIN users u
        ON u.user_id = f2.friend_id

    LEFT JOIN profiles p
        ON p.user_id = u.user_id

    WHERE f1.user_id = :currentUserId
      AND f2.friend_id <> :currentUserId

      AND NOT EXISTS (
          SELECT 1
          FROM friend_pairs f3
          WHERE f3.user_id = :currentUserId
            AND f3.friend_id = f2.friend_id
      )

    GROUP BY
        f2.friend_id,
        u.username,
        p.avatar_url

    ORDER BY mutual_count DESC
    """, nativeQuery = true)
    List<Object[]> findSuggestedUsers(
            @Param("currentUserId") Long currentUserId
    );

    @Query(value = """
    WITH friend_pairs AS (
        SELECT
            user_one_id AS user_id,
            user_two_id AS friend_id
        FROM friendships
        WHERE status = 'ACCEPTED'

        UNION ALL

        SELECT
            user_two_id AS user_id,
            user_one_id AS friend_id
        FROM friendships
        WHERE status = 'ACCEPTED'
    )

    SELECT
        u.user_id AS userId,
        u.username AS username,
        p.avatar_url AS avatarUrl

    FROM friend_pairs f1

    JOIN friend_pairs f2
        ON f1.friend_id = f2.friend_id

    JOIN users u
        ON u.user_id = f1.friend_id

    LEFT JOIN profiles p
        ON p.user_id = u.user_id

    WHERE f1.user_id = :currentUserId
      AND f2.user_id = :targetUserId

    ORDER BY u.username
    """, nativeQuery = true)
    List<Object[]> findMutualFriends(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserId") Long targetUserId
    );
}
