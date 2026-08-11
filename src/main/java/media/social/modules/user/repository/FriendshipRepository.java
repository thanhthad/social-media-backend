package media.social.modules.user.repository;

import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    SELECT new media.social.modules.user.dto.response.user.FriendshipUserResponse(
        u.id,
        u.username,
        u.profile.avatarUrl
    )
    FROM Friendship f
    JOIN User u
        ON (u.id = f.userOne.id OR u.id = f.userTwo.id)
        AND u.id != :userId
    WHERE (f.userOne.id = :userId OR f.userTwo.id = :userId)
      AND f.status = :status
    ORDER BY f.createdAt DESC
    """)
    Page<FriendshipUserResponse> getMyFriends(
            @Param("userId") Long userId,
            @Param("status") FriendshipStatus status,
            Pageable pageable
    );

    @Query("""
    SELECT new media.social.modules.user.dto.response.user.FriendshipUserResponse(
        u.id,
        u.username,
        u.profile.avatarUrl
    )
    FROM Friendship f
    JOIN User u
        ON (u.id = f.userOne.id OR u.id = f.userTwo.id)
        AND u.id != :userId
    WHERE (f.userOne.id = :userId OR f.userTwo.id = :userId)
      AND f.status = :status
      AND (
            f.visibility = :publicVisibility
            OR (
                f.visibility = :friendVisibility
                AND EXISTS (
                    SELECT 1
                    FROM Friendship viewerFriendship
                    WHERE (
                        viewerFriendship.userOne.id = :viewerId
                        AND viewerFriendship.userTwo.id = :userId
                    )
                    OR (
                        viewerFriendship.userOne.id = :userId
                        AND viewerFriendship.userTwo.id = :viewerId
                    )
                    AND viewerFriendship.status = :acceptedStatus
                )
            )
          )
    ORDER BY f.createdAt DESC
    """)
    Page<FriendshipUserResponse> getFriends(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId,
            @Param("status") FriendshipStatus status,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            Pageable pageable
    );
}
