package media.social.modules.user.service;

import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.dto.projection.AdminUserProjection;
import media.social.modules.user.dto.request.user.UpdateUserStatusRequest;
import media.social.modules.user.dto.response.user.AdminUserResponse;
import media.social.modules.user.entity.Role;
import media.social.modules.user.entity.User;
import media.social.modules.user.entity.UserRole;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Mock
    private UserRepository userRepository;

    @Test
    void getAllUsers_success() {
        Status status = Status.ACTIVE;
        Pageable pageable = PageRequest.of(0, 10);

        AdminUserProjection mockProjection = mock(AdminUserProjection.class);
        when(mockProjection.getId()).thenReturn(1L);
        when(mockProjection.getUsername()).thenReturn("testuser");
        when(mockProjection.getStatus()).thenReturn(Status.ACTIVE);
        when(mockProjection.getAvatarUrl()).thenReturn("http://avatar.url");
        LocalDateTime now = LocalDateTime.now();
        when(mockProjection.getCreatedAt()).thenReturn(now);
        when(mockProjection.getLastLoginAt()).thenReturn(now);
        when(mockProjection.getLastActiveAt()).thenReturn(now);

        Page<AdminUserProjection> projectionPage = new PageImpl<>(List.of(mockProjection));
        when(userRepository.findAllAdminUsers(status, pageable)).thenReturn(projectionPage);

        Page<AdminUserResponse> result = adminUserService.getAllUsers(status, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        AdminUserResponse response = result.getContent().get(0);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals(Status.ACTIVE, response.getStatus());
        assertEquals("http://avatar.url", response.getAvatarUrl());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getLastLoginAt());
        assertEquals(now, response.getLastActiveAt());
        verify(userRepository).findAllAdminUsers(status, pageable);
    }

    @Test
    void searchUsers_success() {
        String username = "test";
        Pageable pageable = PageRequest.of(0, 10);
        
        AdminUserProjection mockProjection = mock(AdminUserProjection.class);
        when(mockProjection.getId()).thenReturn(1L);
        when(mockProjection.getUsername()).thenReturn("testuser");
        when(mockProjection.getStatus()).thenReturn(Status.ACTIVE);
        when(mockProjection.getAvatarUrl()).thenReturn("http://avatar.url");
        LocalDateTime now = LocalDateTime.now();
        when(mockProjection.getCreatedAt()).thenReturn(now);
        when(mockProjection.getLastLoginAt()).thenReturn(now);
        when(mockProjection.getLastActiveAt()).thenReturn(now);

        Page<AdminUserProjection> projectionPage = new PageImpl<>(List.of(mockProjection));
        when(userRepository.searchAdminUsers(username, pageable)).thenReturn(projectionPage);

        Page<AdminUserResponse> result = adminUserService.searchUsers(username, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        AdminUserResponse response = result.getContent().get(0);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals(Status.ACTIVE, response.getStatus());
        assertEquals("http://avatar.url", response.getAvatarUrl());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getLastLoginAt());
        assertEquals(now, response.getLastActiveAt());
    }

    @Test
    void updateStatus_success() {
        Long userId = 2L;
        Long currentUserId = 1L;
        
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(Status.BANNED);

        User mockUser = mock(User.class);
        when(mockUser.getUserRoles()).thenReturn(Collections.emptySet());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            adminUserService.updateStatus(userId, request);

            verify(mockUser).setStatus(Status.BANNED);
            verify(userRepository).save(mockUser);
        }
    }

    @Test
    void updateStatus_cannotChangeSelf_throwsAccessDeniedException() {
        Long userId = 1L;
        Long currentUserId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            assertThrows(AccessDeniedException.class, () -> adminUserService.updateStatus(userId, request));
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void updateStatus_userNotFound_throwsUserNotFoundException() {
        Long userId = 2L;
        Long currentUserId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> adminUserService.updateStatus(userId, request));
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updateStatus_cannotChangeAdminStatus_throwsAccessDeniedException() {
        Long userId = 2L;
        Long currentUserId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();

        User mockUser = mock(User.class);
        UserRole mockUserRole = mock(UserRole.class);
        Role mockRole = mock(Role.class);
        
        when(mockRole.getName()).thenReturn(RoleName.ADMIN);
        when(mockUserRole.getRole()).thenReturn(mockRole);
        when(mockUser.getUserRoles()).thenReturn(Set.of(mockUserRole));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            assertThrows(AccessDeniedException.class, () -> adminUserService.updateStatus(userId, request));
            verify(userRepository, never()).save(any());
        }
    }
}
