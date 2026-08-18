package media.social.modules.dating.repository;

import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.dto.response.projection.DatingDiscoveryProjection;
import media.social.modules.dating.dto.response.projection.DatingDistanceProjection;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        ST_Distance(
            CAST(
                ST_SetSRID(
                    ST_MakePoint(
                        me.longitude,
                        me.latitude
                    ),
                    4326
                ) AS geography
            ),
            CAST(
                ST_SetSRID(
                    ST_MakePoint(
                        target.longitude,
                        target.latitude
                    ),
                    4326
                ) AS geography
            )
        ) / 1000.0 AS distance_km

    FROM dating_profiles me

    JOIN dating_profiles target
        ON target.user_id = :targetUserId

    WHERE me.user_id = :currentUserId
    """, nativeQuery = true)
    DatingDistanceProjection findDistanceBetweenUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserId") Long targetUserId
    );

    @Query(
            value = """
            SELECT
                target.user_id AS "userId",
                target.display_name AS "displayName",
                profile.avatar_url AS "avatarUrl",
                CAST(
                    EXTRACT(
                        YEAR FROM AGE(target.birthday)
                    ) AS INTEGER
                ) AS age,
                target.city AS city,
                ST_Distance(
                    CAST(
                        ST_SetSRID(
                            ST_MakePoint(me.longitude, me.latitude),
                            4326
                        ) AS geography
                    ),
                    CAST(
                        ST_SetSRID(
                            ST_MakePoint(target.longitude, target.latitude),
                            4326
                        ) AS geography
                    )
                ) / 1000.0 AS "distanceKm",
                COUNT(DISTINCT common_interest.interest_id)
                    AS "commonInterestCount"

            FROM dating_profiles me

            JOIN dating_preferences my_preference
                ON my_preference.user_id = me.user_id
            JOIN dating_profiles target
                ON target.user_id <> me.user_id
            JOIN profiles profile
                ON profile.user_id = target.user_id
            LEFT JOIN dating_profile_interests my_interest
                ON my_interest.dating_profile_id = me.dating_profile_id
            LEFT JOIN dating_profile_interests common_interest
                ON common_interest.dating_profile_id = target.dating_profile_id
                AND common_interest.interest_id = my_interest.interest_id
            WHERE me.user_id = :currentUserId

              AND target.is_active = TRUE
              AND target.birthday IS NOT NULL

              AND target.latitude IS NOT NULL
              AND target.longitude IS NOT NULL

              AND (
                  my_preference.min_age IS NULL
                  OR EXTRACT(YEAR FROM AGE(target.birthday))
                        >= my_preference.min_age
              )
              AND (
                  my_preference.max_age IS NULL
                  OR EXTRACT(YEAR FROM AGE(target.birthday))
                        <= my_preference.max_age
              )
              AND (
                  my_preference.gender_preference IS NULL
                  OR my_preference.gender_preference = target.gender
              )
               AND (
                   my_preference.max_distance IS NULL
                   OR ST_DWithin(
                       CAST(
                           ST_SetSRID(
                               ST_MakePoint(
                                   me.longitude,
                                   me.latitude
                               ),
                               4326
                           ) AS geography
                       ),
                       CAST(
                           ST_SetSRID(
                               ST_MakePoint(
                                   target.longitude,
                                   target.latitude
                               ),
                               4326
                           ) AS geography
                       ),
                       my_preference.max_distance * 1000
                   )
               )
              AND NOT EXISTS (
                  SELECT 1
                  FROM dating_reports report
                  WHERE (
                      report.reporter_id = :currentUserId
                      AND report.reported_user_id = target.user_id
                  )
                  OR (
                      report.reporter_id = target.user_id
                      AND report.reported_user_id = :currentUserId
                  )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM dating_swipes swipe
                  WHERE swipe.swiper_id = :currentUserId
                    AND swipe.target_id = target.user_id
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM dating_matches match
                  WHERE match.user_one_id =
                        LEAST(:currentUserId, target.user_id)
                    AND match.user_two_id =
                        GREATEST(:currentUserId, target.user_id)
              )
            GROUP BY
                target.user_id,
                target.display_name,
                profile.avatar_url,
                target.birthday,
                target.city,
                target.latitude,
                target.longitude,
                me.latitude,
                me.longitude
            ORDER BY
                "commonInterestCount" DESC,
                "distanceKm" ASC
            """,

            countQuery = """
            SELECT COUNT(*)
            FROM dating_profiles me
            JOIN dating_preferences my_preference
                ON my_preference.user_id = me.user_id
            JOIN dating_profiles target
                ON target.user_id <> me.user_id
            JOIN profiles profile
                ON profile.user_id = target.user_id
            WHERE me.user_id = :currentUserId

              AND target.is_active = TRUE
              AND target.birthday IS NOT NULL

              AND target.latitude IS NOT NULL
              AND target.longitude IS NOT NULL

              AND (
                  my_preference.min_age IS NULL
                  OR EXTRACT(YEAR FROM AGE(target.birthday))
                        >= my_preference.min_age
              )
              AND (
                  my_preference.max_age IS NULL
                  OR EXTRACT(YEAR FROM AGE(target.birthday))
                        <= my_preference.max_age
              )
              AND (
                  my_preference.gender_preference IS NULL
                  OR my_preference.gender_preference = target.gender
              )
                AND (
                    my_preference.max_distance IS NULL
                    OR ST_DWithin(
                        CAST(
                            ST_SetSRID(
                                ST_MakePoint(
                                    me.longitude,
                                    me.latitude
                                ),
                                4326
                            ) AS geography
                        ),
                        CAST(
                            ST_SetSRID(
                                ST_MakePoint(
                                    target.longitude,
                                    target.latitude
                                ),
                                4326
                            ) AS geography
                        ),
                        my_preference.max_distance * 1000
                    )
                )
             AND NOT EXISTS (
                         SELECT 1
                         FROM dating_reports report
                         WHERE (
                             report.reporter_id = :currentUserId
                             AND report.reported_user_id = target.user_id
                         )
                         OR (
                             report.reporter_id = target.user_id
                             AND report.reported_user_id = :currentUserId
                         )
                     )
              AND NOT EXISTS (
                  SELECT 1
                  FROM dating_swipes swipe
                  WHERE swipe.swiper_id = :currentUserId
                    AND swipe.target_id = target.user_id
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM dating_matches match
                  WHERE match.user_one_id =
                        LEAST(:currentUserId, target.user_id)
                    AND match.user_two_id =
                        GREATEST(:currentUserId, target.user_id)
              )
            """,
            nativeQuery = true
    )
    Page<DatingDiscoveryProjection> findDiscoveryCandidates(
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

}