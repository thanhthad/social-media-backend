package media.social.modules.user.repository;

import media.social.modules.auth.Enum.Status;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.cache.UserFollowStatCacheResponse;
import media.social.modules.user.dto.response.user.AdminUserResponse;
import media.social.modules.user.dto.response.user.UserSearchResponse;
import media.social.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
    SELECT new media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse(
        u.id,
        u.email,
        u.username,
        p.avatarUrl,
        p.coverUrl,
        p.bio,
        p.fullName,
        p.website,
        p.phone,
        p.dateOfBirth,
        p.gender,
        p.country,
        p.city,
        p.district,
        p.occupation,
        p.company,
        p.education,
        p.profileVisibility,
        p.socialLinks,
        p.createdAt,
        p.updatedAt
    )
    FROM User u
    LEFT JOIN Profile p
        ON p.user.id = u.id
    WHERE u.id = :userId
    """)
    Optional<PublicUserProfileCacheResponse> findCurrentUserProfileCache(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT new media.social.modules.user.dto.response.cache.UserFollowStatCacheResponse(
        COUNT(DISTINCT follower.id),
        COUNT(DISTINCT following.id)
    )
    
    FROM User u
    
    LEFT JOIN Follow follower
    ON follower.following.id = u.id
    
    LEFT JOIN Follow following
    ON following.follower.id = u.id
    
    WHERE u.id = :userId
    
    GROUP BY u.id
    """)
    Optional<UserFollowStatCacheResponse> findFollowStat(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT DISTINCT u
    FROM User u
    LEFT JOIN FETCH u.userRoles ur
    LEFT JOIN FETCH ur.role
    WHERE u.email = :email
    """)
    Optional<User> findByEmailWithRoles(String email);


    @Query("""
    select u
    from User u
    left join fetch u.profile
    where u.id = :userId
""")
    Optional<User> findByIdWithProfile(
            @Param("userId") Long userId
    );

    Optional<User> findByUsername(String userName);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String userName);

    boolean existsByEmail(String email);

    @Query("""
    SELECT new media.social.modules.user.dto.response.user.UserSearchResponse(
        u.id,
        u.username,
        p.avatarUrl,
        p.fullName
    )
    FROM User u
    JOIN u.profile p
    WHERE u.username ILIKE CONCAT('%', :username, '%')
    AND u.status = :status
    """)
    Page<UserSearchResponse> searchUsers(
            @Param("username") String username,
            @Param("status") Status status,
            Pageable pageable
    );

    @Query("""
    SELECT new media.social.modules.user.dto.response.user.AdminUserResponse(
        u.id,
        u.username,
        u.status,
        p.avatarUrl,
        u.createdAt,
        u.lastLoginAt,
        u.lastActiveAt
    )
    FROM User u
    LEFT JOIN u.profile p
    WHERE (:status IS NULL OR u.status = :status)
    ORDER BY u.createdAt DESC
    """)
    Page<AdminUserResponse> findAllAdminUsers(
            @Param("status") Status status,
            Pageable pageable
    );

    @Query("""
    SELECT new media.social.modules.user.dto.response.user.AdminUserResponse(
        u.id,
        u.username,
        u.status,
        p.avatarUrl,
        u.createdAt,
        u.lastLoginAt,
        u.lastActiveAt
    )
    FROM User u
    LEFT JOIN u.profile p
    WHERE u.username ILIKE CONCAT('%', :username, '%')
    ORDER BY u.createdAt DESC
    """)
    Page<AdminUserResponse> searchAdminUsers(
            @Param("username") String username,
            Pageable pageable
    );
}