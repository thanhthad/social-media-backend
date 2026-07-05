package media.social.modults.user.repository;

import media.social.modults.user.dto.response.user.FollowUserResponse;
import media.social.modults.user.entity.Follow;
import media.social.modults.user.entity.FollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByFollower_IdAndFollowing_Id(
            Long followerId,
            Long followingId
    );


    long deleteByFollower_IdAndFollowing_Id(
            Long followerId,
            Long followingId
    );

    long countByFollower_Id(Long userId);

    long countByFollowing_Id(Long userId);

    @Query("""
        SELECT new media.social.modults.user.dto.response.user.FollowUserResponse(
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

    @Query("""
        SELECT new media.social.modults.user.dto.response.user.FollowUserResponse(
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

