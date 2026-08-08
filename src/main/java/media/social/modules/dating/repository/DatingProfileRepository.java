package media.social.modules.dating.repository;

import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.dto.response.projection.DatingDistanceProjection;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DatingProfileRepository
        extends JpaRepository<DatingProfile, Long> {

    Optional<DatingProfile> findByUser(User user);

    Optional<DatingProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
    SELECT new media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse(
        u.username,
        p.avatarUrl,
        p.coverUrl,
        dp.displayName,
        dp.bio,
        dp.gender,
        dp.birthday,
        dp.height,
        dp.occupation,
        dp.education,
        dp.country,
        dp.city,
        dp.district,
        dp.active,
        dp.visibility,
        dp.createdAt,
        dp.updatedAt
    )
    FROM DatingProfile dp
    JOIN dp.user u
    LEFT JOIN u.profile p
    WHERE u.id = :userId
    """)
    Optional<DatingProfileCacheResponse> findDatingProfileCache(Long userId);

    @Query(value = """
        SELECT
            CASE
                WHEN me.latitude IS NULL
                  OR me.longitude IS NULL
                THEN 'CURRENT_USER_LOCATION_MISSING'

                WHEN target.latitude IS NULL
                  OR target.longitude IS NULL
                THEN 'TARGET_USER_LOCATION_MISSING'

                ELSE 'OK'
            END AS location_status,

            CASE
                WHEN me.latitude IS NULL
                  OR me.longitude IS NULL
                  OR target.latitude IS NULL
                  OR target.longitude IS NULL
                THEN NULL

                ELSE ST_Distance(
                    ST_SetSRID(
                        ST_MakePoint(
                            me.longitude,
                            me.latitude
                        ),
                        4326
                    )::geography,

                    ST_SetSRID(
                        ST_MakePoint(
                            target.longitude,
                            target.latitude
                        ),
                        4326
                    )::geography
                ) / 1000.0
            END AS distance_km

        FROM dating_profiles me
        JOIN dating_profiles target
            ON target.user_id = :targetUserId

        WHERE me.user_id = :currentUserId
        """, nativeQuery = true)
    DatingDistanceProjection findDistanceBetweenUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserId") Long targetUserId
    );

}