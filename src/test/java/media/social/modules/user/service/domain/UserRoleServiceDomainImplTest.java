package media.social.modules.user.service.domain;

import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.dto.response.role.RoleResponse;
import media.social.modules.user.entity.Role;
import media.social.modules.user.repository.UserRoleRepository;
import media.social.modules.user.service.domain.impl.UserRoleServiceDomainImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceDomainImplTest {

    @InjectMocks
    private UserRoleServiceDomainImpl userRoleServiceDomain;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Test
    void getUserRoles_success() {
        Long userId = 1L;
        Role role = Role.builder()
                .name(RoleName.ADMIN)
                .description("Administrator Role")
                .build();

        when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of(role));

        List<RoleResponse> result = userRoleServiceDomain.getUserRoles(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("Administrator Role", result.get(0).getDescription());
    }

    @Test
    void getUserRoles_emptyList_returnsEmptyList() {
        Long userId = 1L;
        when(userRoleRepository.findRolesByUserId(userId)).thenReturn(Collections.emptyList());

        List<RoleResponse> result = userRoleServiceDomain.getUserRoles(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyRoles_success() {
        Long currentUserId = 1L;
        Role role = Role.builder()
                .name(RoleName.USER)
                .description("Regular User")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(userRoleRepository.findRolesByUserId(currentUserId)).thenReturn(List.of(role));

            List<RoleResponse> result = userRoleServiceDomain.getMyRoles();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("USER", result.get(0).getName());
            assertEquals("Regular User", result.get(0).getDescription());
        }
    }

    @Test
    void hasRole_returnsTrue_whenUserHasRole() {
        Long userId = 1L;
        RoleName roleName = RoleName.MODERATOR;
        when(userRoleRepository.existsByUserIdAndRoleName(userId, roleName)).thenReturn(true);

        boolean result = userRoleServiceDomain.hasRole(userId, roleName);

        assertTrue(result);
        verify(userRoleRepository).existsByUserIdAndRoleName(userId, roleName);
    }

    @Test
    void hasRole_returnsFalse_whenUserDoesNotHaveRole() {
        Long userId = 1L;
        RoleName roleName = RoleName.MODERATOR;
        when(userRoleRepository.existsByUserIdAndRoleName(userId, roleName)).thenReturn(false);

        boolean result = userRoleServiceDomain.hasRole(userId, roleName);

        assertFalse(result);
        verify(userRoleRepository).existsByUserIdAndRoleName(userId, roleName);
    }
}
