package media.social.modules.user.service;

import media.social.modules.user.Enum.RoleName;
import media.social.modules.user.dto.response.role.RoleResponse;

import java.util.List;

public interface UserRoleService {

    void assignRole(Long userId , RoleName roleName);

    void removeRole(Long userId, RoleName roleName);

    List<RoleResponse> getUserRoles(Long userId);

    List<RoleResponse> getMyRoles();

    boolean hasRole(Long userId,RoleName roleName);
}