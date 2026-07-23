package media.social.modults.user.service.domain;

import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.dto.response.role.RoleResponse;

import java.util.List;

public interface UserRoleServiceDomain {

    List<RoleResponse> getUserRoles(Long userId);

    List<RoleResponse> getMyRoles();

    boolean hasRole(Long userId, RoleName roleName);
}
