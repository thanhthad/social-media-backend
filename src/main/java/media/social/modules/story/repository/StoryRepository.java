package media.social.modules.story.repository;

import media.social.modules.auth.Enum.Status;
import media.social.modules.post.enums.Visibility;
import media.social.modules.story.dto.projection.StoryFeedProjection;
import media.social.modules.story.dto.projection.UserStoryProjection;
import media.social.modules.story.dto.response.StoryFeedResponse;
import media.social.modules.story.dto.response.UserStoryResponse;
import media.social.modules.story.entity.Story;
import media.social.modules.user.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("""
        SELECT s
        FROM Story s
        WHERE s.id = :storyId
        AND s.expiresAt > :now
        """)
    Optional<Story> findActiveById(
            @Param("storyId") Long storyId,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT s
        FROM Story s
        WHERE s.user.id = :userId
        AND s.expiresAt > :now
        ORDER BY s.createdAt DESC
        """)
    Page<Story> findActiveByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    Optional<Story> findByIdAndExpiresAtAfter(
            Long storyId,
            LocalDateTime now
    );

    @Query("""
        SELECT DISTINCT s
        FROM Story s
        JOIN FETCH s.user u
        JOIN FETCH s.media
        WHERE s.id = :storyId
        """)
    Optional<Story> findByIdWithUserAndMedia(
            @Param("storyId") Long storyId
    );

    @Query("""
        SELECT
            s.user.id AS userId,
            pr.avatarUrl AS avatarUrl,
            s.content AS content,
            s.visibility AS visibility,
            sm.mediaType AS mediaType,
            sm.url AS url
        FROM Story s
        JOIN s.user u
        JOIN u.profile pr
        JOIN s.media sm
    
        WHERE s.expiresAt > :now
    
        AND u.status = :activeStatus
    
        AND NOT EXISTS (
            SELECT 1
            FROM Block b
            WHERE
                (b.blocker.id = :viewerId AND b.blocked.id = u.id)
                OR
                (b.blocker.id = u.id AND b.blocked.id = :viewerId)
        )
    
        AND (
            u.id = :viewerId
    
            OR s.visibility = :publicVisibility
    
            OR (
                s.visibility = :friendVisibility
    
                AND EXISTS (
                    SELECT 1
                    FROM Friendship f
                    WHERE
                        (
                            (f.userOne.id = :viewerId AND f.userTwo.id = u.id)
                            OR
                            (f.userOne.id = u.id AND f.userTwo.id = :viewerId)
                        )
                        AND f.status = :acceptedStatus
                )
            )
        )
    
        AND s.createdAt = (
            SELECT MAX(s2.createdAt)
            FROM Story s2
            WHERE s2.user.id = s.user.id
            AND s2.expiresAt > :now
    
            AND (
                s2.visibility = :publicVisibility
    
                OR (
                    s2.visibility = :friendVisibility
    
                    AND EXISTS (
                        SELECT 1
                        FROM Friendship f2
                        WHERE
                            (
                                (f2.userOne.id = :viewerId AND f2.userTwo.id = s2.user.id)
                                OR
                                (f2.userOne.id = s2.user.id AND f2.userTwo.id = :viewerId)
                            )
                            AND f2.status = :acceptedStatus
                    )
                )
    
                OR s2.user.id = :viewerId
            )
        )
    
        ORDER BY s.createdAt DESC
    """)
    List<StoryFeedProjection> findFeed(
            @Param("viewerId") Long viewerId,
            @Param("now") LocalDateTime now,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("activeStatus") Status activeStatus
    );

    @Query("""
        SELECT
            s.id AS storyId,
            s.user.id AS userId,
            u.username AS username,
            pr.avatarUrl AS avatarUrl,
            s.content AS content,
            s.visibility AS visibility,
            sm.mediaType AS mediaType,
            sm.url AS url,
            s.expiresAt AS expiresAt,
            s.createdAt AS createdAt,
            s.updatedAt AS updatedAt
    
        FROM Story s
        JOIN s.user u
        JOIN u.profile pr
        JOIN s.media sm
    
        WHERE s.user.id = :targetUserId
          AND s.expiresAt > :now
          AND u.status = :activeStatus
    
          AND (
              s.user.id = :viewerId
    
              OR s.visibility = :publicVisibility
    
              OR (
                  s.visibility = :friendVisibility
    
                  AND EXISTS (
                      SELECT 1
                      FROM Friendship f
                      WHERE
                          (
                              (f.userOne.id = :viewerId AND f.userTwo.id = :targetUserId)
                              OR
                              (f.userOne.id = :targetUserId AND f.userTwo.id = :viewerId)
                          )
                          AND f.status = :acceptedStatus
                  )
              )
          )
    
        ORDER BY s.createdAt ASC
    """)
    List<UserStoryProjection> findUserStories(
            @Param("viewerId") Long viewerId,
            @Param("targetUserId") Long targetUserId,
            @Param("now") LocalDateTime now,
            @Param("acceptedStatus") FriendshipStatus acceptedStatus,
            @Param("publicVisibility") Visibility publicVisibility,
            @Param("friendVisibility") Visibility friendVisibility,
            @Param("activeStatus") Status activeStatus
    );

    @Query("""
        SELECT
            s.id AS storyId,
            s.user.id AS userId,
            u.username AS username,
            pr.avatarUrl AS avatarUrl,
            s.content AS content,
            s.visibility AS visibility,
            sm.mediaType AS mediaType,
            sm.url AS url,
            s.expiresAt AS expiresAt,
            s.createdAt AS createdAt,
            s.updatedAt AS updatedAt
    
        FROM Story s
        JOIN s.user u
        JOIN u.profile pr
        JOIN s.media sm
    
        WHERE s.user.id = :userId
          AND s.expiresAt > :now
          AND u.status = :activeStatus
    
        ORDER BY s.createdAt ASC
    """)
    List<UserStoryProjection> findActiveStoriesByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("activeStatus") Status activeStatus
    );
}