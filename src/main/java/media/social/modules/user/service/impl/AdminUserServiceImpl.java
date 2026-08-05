package media.social.modules.user.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.user.dto.request.user.UpdateUserStatusRequest;
import media.social.modules.user.dto.response.user.AdminUserResponse;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.AdminUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@AllArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(
            Status status,
            Pageable pageable
    ) {
        return userRepository.findAllAdminUsers(
                status,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> searchUsers(
            String username,
            Pageable pageable
    ) {
        return userRepository.searchAdminUsers(
                username,
                pageable
        );
    }

    @Override
    @Transactional
    public void updateStatus(
            Long userId,
            UpdateUserStatusRequest request
    ) {
        Long currentUserId = UserContextHolder.getUserId();

        if (currentUserId.equals(userId)) {
            throw new AccessDeniedException(
                    "You cannot change your own status"
            );
        }
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found"
                        )
                );
        boolean isAdmin = user.getUserRoles()
                .stream()
                .anyMatch(userRole ->
                        userRole.getRole()
                                .getName()
                                .equals(RoleName.ADMIN)
                );
        if (isAdmin) {
            throw new AccessDeniedException(
                    "You cannot change admin status"
            );
        }
        user.setStatus(
                request.getStatus()
        );
        userRepository.save(user);
    }
}