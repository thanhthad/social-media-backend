package media.social.modules.user.repository;

import media.social.modules.auth.Enum.RoleName;
import media.social.modules.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

}