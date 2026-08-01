package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modults.user.Enum.Status;
import media.social.modults.user.dto.request.user.UpdateUserStatusRequest;
import media.social.modults.user.service.AdminUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // ================= GET ALL USERS =================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all users")
    @RateLimit(
            name = "ADMIN_GET_USERS",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) Status status,
            Pageable pageable
    ) {

        return ResponseData.success(
                adminUserService.getAllUsers(
                        status,
                        pageable
                ),
                "Get all users successfully",
                HttpStatus.OK
        );
    }

    // ================= SEARCH USERS =================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    @Operation(summary = "Search users by username")
    @RateLimit(
            name = "ADMIN_SEARCH_USERS",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> searchUsers(
            @RequestParam String username,
            Pageable pageable
    ) {

        return ResponseData.success(
                adminUserService.searchUsers(
                        username,
                        pageable
                ),
                "Search users successfully",
                HttpStatus.OK
        );
    }

    // ================= UPDATE USER STATUS =================

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status")
    @RateLimit(
            name = "ADMIN_UPDATE_USER_STATUS",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserStatusRequest request
    ) {

        adminUserService.updateStatus(
                userId,
                request
        );

        return ResponseData.success(
                null,
                "Update user status successfully",
                HttpStatus.OK
        );
    }
}