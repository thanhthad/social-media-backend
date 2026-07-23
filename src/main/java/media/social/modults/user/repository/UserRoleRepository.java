package media.social.modults.user.repository;

import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.entity.Role;
import media.social.modults.user.entity.User;
import media.social.modults.user.entity.UserRole;
import media.social.modults.user.entity.UserRoleId;
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
    boolean existsUserRole(Long userId, String roleName);

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
    List<User> findUsersByRole(String roleName);

    boolean existsByUser_IdAndRole_Name(Long userId, String roleName);
}