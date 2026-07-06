package media.social.modults.user.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.dto.response.role.RoleResponse;
import media.social.modults.user.entity.Role;
import media.social.modults.user.entity.User;
import media.social.modults.user.entity.UserRole;
import media.social.modults.user.entity.UserRoleId;
import media.social.modults.user.exception.role.RoleNotFoundException;
import media.social.modults.user.repository.RoleRepository;
import media.social.modults.user.repository.UserRoleRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.UserRoleService;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserServiceDomain userServiceDomain;
    private final RoleRepository roleRepository;

    @Override
    public void assignRole(Long userId, RoleName roleName) {

        User user = userServiceDomain.getByUserId(userId);

        Role role = roleRepository.findByName(roleName.name())
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        if (userRoleRepository.existsByUser_IdAndRole_Name(userId, roleName.name())) {
            throw new RuntimeException("User already has this role");
        }

        Long adminId = UserContextHolder.getUserId();
        User assignedByUser = userServiceDomain.getByUserId(adminId);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(userId, role.getId()))
                .user(user)
                .role(role)
                .assignedBy(assignedByUser)
                .build();

        userRoleRepository.save(userRole);
    }

    @Override
    public void removeRole(Long userId, RoleName roleName) {

        Role role = roleRepository.findByName(roleName.name())
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        if (!userRoleRepository.existsByUser_IdAndRole_Name(userId, roleName.name())) {
            throw new RuntimeException("User does not have this role");
        }

        userRoleRepository.deleteById(new UserRoleId(userId, role.getId()));
    }

    @Override
    public List<RoleResponse> getUserRoles(Long userId) {

        return userRoleRepository.findRolesByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<RoleResponse> getMyRoles() {

        Long userId = UserContextHolder.getUserId();

        return getUserRoles(userId);
    }

    @Override
    public boolean hasRole(Long userId, RoleName roleName) {

        return userRoleRepository.existsByUser_IdAndRole_Name(
                userId,
                roleName.name()
        );
    }

    private RoleResponse mapToResponse(Role role) {

        return RoleResponse.builder()
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}