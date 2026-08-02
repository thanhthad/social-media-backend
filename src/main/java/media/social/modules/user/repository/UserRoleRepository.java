package media.social.modules.user.repository;

import media.social.modules.user.Enum.RoleName;
import media.social.modules.user.entity.Role;
import media.social.modules.user.entity.User;
import media.social.modules.user.entity.UserRole;
import media.social.modules.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByUserIdAndRoleName(
            Long userId,
            RoleName roleName
    );

    @Query("""
    SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END
    FROM UserRole ur
    WHERE ur.user.id = :userId
    AND ur.role.name = :roleName
""")
    boolean existsUserRole(Long userId, RoleName roleName);

    @Query("""
    SELECT r
    FROM UserRole ur
    JOIN ur.role r
    WHERE ur.user.id = :userId
""")
    List<Role> findRolesByUserId(Long userId);

    @Query("""
    SELECT u
    FROM UserRole ur
    JOIN ur.user u
    WHERE ur.role.name = :roleName
""")
    List<User> findUsersByRole(RoleName roleName);

    boolean existsByUser_IdAndRole_Name(Long userId, RoleName roleName);
}