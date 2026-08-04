package media.social.modules.user.service.domain;

import media.social.modules.auth.enums.RoleName;
import media.social.modules.user.dto.response.role.RoleResponse;

import java.util.List;

public interface UserRoleServiceDomain {

    List<RoleResponse> getUserRoles(Long userId);

    List<RoleResponse> getMyRoles();

    boolean hasRole(Long userId, RoleName roleName);
}
