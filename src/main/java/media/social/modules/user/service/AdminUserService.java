package media.social.modules.user.service;

import media.social.modules.auth.enums.Status;
import media.social.modules.user.dto.request.user.UpdateUserStatusRequest;
import media.social.modules.user.dto.response.user.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponse> getAllUsers(
            Status status,
            Pageable pageable
    );


    Page<AdminUserResponse> searchUsers(
            String username,
            Pageable pageable
    );


    void updateStatus(
            Long userId,
            UpdateUserStatusRequest request
    );

}