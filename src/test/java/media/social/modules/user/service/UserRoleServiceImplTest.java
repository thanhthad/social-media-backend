package media.social.modules.user.service;

import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.dto.response.role.RoleResponse;
import media.social.modules.user.entity.Role;
import media.social.modules.user.entity.User;
import media.social.modules.user.entity.UserRole;
import media.social.modules.user.entity.UserRoleId;
import media.social.modules.user.exception.role.RoleNotFoundException;
import media.social.modules.user.exception.role.UserRoleAlreadyExistsException;
import media.social.modules.user.exception.role.UserRoleNotFoundException;
import media.social.modules.user.repository.RoleRepository;
import media.social.modules.user.repository.UserRoleRepository;
import media.social.modules.user.service.domain.UserServiceDomain;
import media.social.modules.user.service.impl.UserRoleServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private RoleRepository roleRepository;

    // =========================================================
    // assignRole()
    // =========================================================

    @Test
    void assignRole_success() {
        Long targetUserId = 2L;
        Long adminId = 1L;

        User targetUser = new User();
        targetUser.setId(targetUserId);

        User adminUser = new User();
        adminUser.setId(adminId);

        Role role = Role.builder()
                .id(10L)
                .name(RoleName.MODERATOR)
                .description("Moderator role")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(targetUserId)).thenReturn(targetUser);
            when(roleRepository.findByName(RoleName.MODERATOR)).thenReturn(Optional.of(role));
            when(userRoleRepository.existsByUser_IdAndRole_Name(targetUserId, RoleName.MODERATOR))
                    .thenReturn(false);
            when(userServiceDomain.getByUserId(adminId)).thenReturn(adminUser);

            userRoleService.assignRole(targetUserId, RoleName.MODERATOR);

            ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
            verify(userRoleRepository).save(captor.capture());

            UserRole saved = captor.getValue();
            assertEquals(targetUser, saved.getUser());
            assertEquals(role, saved.getRole());
            assertEquals(adminUser, saved.getAssignedBy());
            assertEquals(new UserRoleId(targetUserId, role.getId()), saved.getId());
        }
    }

    @Test
    void assignRole_roleNotFound_throwsRoleNotFoundException() {
        Long targetUserId = 2L;
        Long adminId = 1L;

        User targetUser = new User();
        targetUser.setId(targetUserId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(targetUserId)).thenReturn(targetUser);
            when(roleRepository.findByName(RoleName.MODERATOR)).thenReturn(Optional.empty());

            assertThrows(RoleNotFoundException.class,
                    () -> userRoleService.assignRole(targetUserId, RoleName.MODERATOR));

            verify(userRoleRepository, never()).save(any());
        }
    }

    @Test
    void assignRole_userAlreadyHasRole_throwsUserRoleAlreadyExistsException() {
        Long targetUserId = 2L;
        Long adminId = 1L;

        User targetUser = new User();
        targetUser.setId(targetUserId);

        Role role = Role.builder()
                .id(10L)
                .name(RoleName.MODERATOR)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(targetUserId)).thenReturn(targetUser);
            when(roleRepository.findByName(RoleName.MODERATOR)).thenReturn(Optional.of(role));
            when(userRoleRepository.existsByUser_IdAndRole_Name(targetUserId, RoleName.MODERATOR))
                    .thenReturn(true);

            assertThrows(UserRoleAlreadyExistsException.class,
                    () -> userRoleService.assignRole(targetUserId, RoleName.MODERATOR));

            verify(userRoleRepository, never()).save(any());
        }
    }

    // =========================================================
    // removeRole()
    // =========================================================

    @Test
    void removeRole_success() {
        Long targetUserId = 2L;

        Role role = Role.builder()
                .id(10L)
                .name(RoleName.MODERATOR)
                .build();

        when(roleRepository.findByName(RoleName.MODERATOR)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUser_IdAndRole_Name(targetUserId, RoleName.MODERATOR))
                .thenReturn(true);

        userRoleService.removeRole(targetUserId, RoleName.MODERATOR);

        verify(userRoleRepository).deleteById(new UserRoleId(targetUserId, role.getId()));
    }

    @Test
    void removeRole_roleNotFound_throwsRoleNotFoundException() {
        Long targetUserId = 2L;

        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> userRoleService.removeRole(targetUserId, RoleName.ADMIN));

        verify(userRoleRepository, never()).deleteById(any());
    }

    @Test
    void removeRole_userDoesNotHaveRole_throwsUserRoleNotFoundException() {
        Long targetUserId = 2L;

        Role role = Role.builder()
                .id(10L)
                .name(RoleName.MODERATOR)
                .build();

        when(roleRepository.findByName(RoleName.MODERATOR)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUser_IdAndRole_Name(targetUserId, RoleName.MODERATOR))
                .thenReturn(false);

        assertThrows(UserRoleNotFoundException.class,
                () -> userRoleService.removeRole(targetUserId, RoleName.MODERATOR));

        verify(userRoleRepository, never()).deleteById(any());
    }

    // =========================================================
    // getUserRoles()
    // =========================================================

    @Test
    void getUserRoles_success_returnsRoleList() {
        Long userId = 1L;

        Role adminRole = Role.builder()
                .id(1L)
                .name(RoleName.ADMIN)
                .description("Administrator")
                .build();

        Role userRole = Role.builder()
                .id(2L)
                .name(RoleName.USER)
                .description("Regular user")
                .build();

        when(userRoleRepository.findRolesByUserId(userId))
                .thenReturn(List.of(adminRole, userRole));

        List<RoleResponse> result = userRoleService.getUserRoles(userId);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("Administrator", result.get(0).getDescription());

        assertEquals("USER", result.get(1).getName());
        assertEquals("Regular user", result.get(1).getDescription());
    }

    @Test
    void getUserRoles_noRoles_returnsEmptyList() {
        Long userId = 1L;

        when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());

        List<RoleResponse> result = userRoleService.getUserRoles(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =========================================================
    // getMyRoles()
    // =========================================================

    @Test
    void getMyRoles_success() {
        Long userId = 1L;

        Role moderatorRole = Role.builder()
                .id(3L)
                .name(RoleName.MODERATOR)
                .description("Moderator")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userRoleRepository.findRolesByUserId(userId))
                    .thenReturn(List.of(moderatorRole));

            List<RoleResponse> result = userRoleService.getMyRoles();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("MODERATOR", result.get(0).getName());
            assertEquals("Moderator", result.get(0).getDescription());

            verify(userRoleRepository).findRolesByUserId(userId);
        }
    }

    @Test
    void getMyRoles_noRoles_returnsEmptyList() {
        Long userId = 1L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());

            List<RoleResponse> result = userRoleService.getMyRoles();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // =========================================================
    // hasRole()
    // =========================================================

    @Test
    void hasRole_returnsTrue_whenUserHasRole() {
        Long userId = 1L;

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleName.ADMIN))
                .thenReturn(true);

        boolean result = userRoleService.hasRole(userId, RoleName.ADMIN);

        assertTrue(result);
        verify(userRoleRepository).existsByUser_IdAndRole_Name(userId, RoleName.ADMIN);
    }

    @Test
    void hasRole_returnsFalse_whenUserDoesNotHaveRole() {
        Long userId = 1L;

        when(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleName.ADMIN))
                .thenReturn(false);

        boolean result = userRoleService.hasRole(userId, RoleName.ADMIN);

        assertFalse(result);
        verify(userRoleRepository).existsByUser_IdAndRole_Name(userId, RoleName.ADMIN);
    }
}
