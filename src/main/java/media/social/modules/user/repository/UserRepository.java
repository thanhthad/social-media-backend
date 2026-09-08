package media.social.modules.user.repository;

import media.social.modules.auth.Enum.Status;
import media.social.modules.user.dto.projection.AdminUserProjection;
import media.social.modules.user.dto.projection.PublicUserProfileCacheProjection;
import media.social.modules.user.dto.projection.UserSearchProjection;
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
    SELECT
        u.id AS id,
        u.email AS email,
        u.username AS username,

        p.avatarUrl AS avatarUrl,
        p.coverUrl AS coverUrl,
        p.bio AS bio,
        p.fullName AS fullName,
        p.website AS website,
        p.phone AS phone,
        p.dateOfBirth AS dateOfBirth,
        p.gender AS gender,
        p.country AS country,
        p.city AS city,
        p.district AS district,
        p.occupation AS occupation,
        p.company AS company,
        p.education AS education,
        p.visibility AS visibility,
        p.socialLinks AS socialLinks,
        p.createdAt AS createdAt,
        p.updatedAt AS updatedAt

    FROM User u
    LEFT JOIN Profile p
        ON p.user.id = u.id

    WHERE u.id = :userId
""")
    Optional<PublicUserProfileCacheProjection> findCurrentUserProfileCache(
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

    @Query(value = """
        SELECT 
            u.user_id AS id,
            u.username AS username,
            p.avatar_url AS avatarUrl,
            p.full_name AS fullName
    
        FROM users u
    
        JOIN profiles p 
            ON p.user_id = u.user_id
    
        WHERE similarity(u.username, :username) > 0.3
    
        AND u.status = :status
    
        AND NOT EXISTS (
            SELECT 1
            FROM blocks b
            WHERE 
                (b.blocker_id = :viewerId 
                 AND b.blocked_id = u.user_id)
                OR
                (b.blocker_id = u.user_id 
                 AND b.blocked_id = :viewerId)
        )
    
        ORDER BY similarity(u.username, :username) DESC
    """,
                countQuery = """
        SELECT COUNT(*)
        FROM users u
    
        WHERE similarity(u.username, :username) > 0.3
    
        AND u.status = :status
    
        AND NOT EXISTS (
            SELECT 1
            FROM blocks b
            WHERE 
                (b.blocker_id = :viewerId 
                 AND b.blocked_id = u.user_id)
    
                OR
    
                (b.blocker_id = u.user_id 
                 AND b.blocked_id = :viewerId)
        )
    """,
            nativeQuery = true)
    Page<UserSearchProjection> searchUsers(
            @Param("username") String username,
            @Param("status") String status,
            @Param("viewerId") Long viewerId,
            Pageable pageable
    );

    @Query("""
    SELECT
        u.id AS id,
        u.username AS username,
        u.status AS status,
        p.avatarUrl AS avatarUrl,
        u.createdAt AS createdAt,
        u.lastLoginAt AS lastLoginAt,
        u.lastActiveAt AS lastActiveAt
    FROM User u
    LEFT JOIN u.profile p
    WHERE (:status IS NULL OR u.status = :status)
    ORDER BY u.createdAt DESC
""")
    Page<AdminUserProjection> findAllAdminUsers(
            @Param("status") Status status,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT
            u.user_id AS id,
            u.username AS username,
            u.status AS status,
            p.avatar_url AS avatarUrl,
            u.created_at AS createdAt,
            u.last_login_at AS lastLoginAt,
            u.last_active_at AS lastActiveAt

        FROM users u

        LEFT JOIN profiles p
            ON p.user_id = u.user_id

        WHERE similarity(u.username, :username) > 0.3

        ORDER BY similarity(u.username, :username) DESC,
                 u.created_at DESC
        """,

            countQuery = """
        SELECT COUNT(*)

        FROM users u

        WHERE similarity(u.username, :username) > 0.3
        """,

            nativeQuery = true
    )
    Page<AdminUserProjection> searchAdminUsers(
            @Param("username") String username,
            Pageable pageable
    );
}