package media.social.modults.user.repository;

import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

}