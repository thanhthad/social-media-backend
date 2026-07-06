package media.social.modults.user.service;

import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.dto.response.role.RoleResponse;

import java.util.List;

public interface UserRoleService {

    void assignRole(Long userId , RoleName roleName);

    void removeRole(Long userId, RoleName roleName);

    List<RoleResponse> getUserRoles(Long userId);

    List<RoleResponse> getMyRoles();

    boolean hasRole(Long userId,RoleName roleName);
}