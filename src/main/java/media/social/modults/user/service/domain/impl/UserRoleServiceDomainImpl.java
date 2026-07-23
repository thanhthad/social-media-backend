package media.social.modults.user.service.domain.impl;

import lombok.AllArgsConstructor;
import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.dto.response.role.RoleResponse;
import media.social.modults.user.repository.UserRoleRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserRoleServiceDomain;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserRoleServiceDomainImpl implements UserRoleServiceDomain {
    private final UserRoleRepository userRoleRepository;

    @Override
    public List<RoleResponse> getUserRoles(Long userId) {

        return userRoleRepository.findRolesByUserId(userId)
                .stream()
                .map(role -> RoleResponse.builder()
                        .name(role.getName().toString())
                        .description(role.getDescription())
                        .build()
                )
                .toList();
    }

    @Override
    public List<RoleResponse> getMyRoles() {

        Long userId = UserContextHolder.getUserId();

        return getUserRoles(userId);
    }

    @Override
    public boolean hasRole(Long userId, RoleName roleName){

        return userRoleRepository.existsByUserIdAndRoleName(
                userId,
                roleName
        );
    }
}
