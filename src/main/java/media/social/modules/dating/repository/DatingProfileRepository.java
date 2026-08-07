package media.social.modules.dating.repository;

import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

}