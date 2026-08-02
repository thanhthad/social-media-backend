package media.social.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.user.Enum.RoleName;
import media.social.modules.user.dto.response.role.RoleResponse;
import media.social.modules.user.service.UserRoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user-roles")
@RequiredArgsConstructor
@Tag(name = "User Role Controller", description = "User Role Management APIs")
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping("/{userId}/{roleName}")
    @Operation(summary = "Assign role to user")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(
            name = "ADMIN_ASSIGN_ROLE",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> assignRole(
            @PathVariable Long userId,
            @PathVariable RoleName roleName
    ) {

        userRoleService.assignRole(userId, roleName);

        return ResponseData.success(
                null,
                "Assign role successfully",
                HttpStatus.OK
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}/{roleName}")
    @Operation(summary = "Remove role from user")
    @RateLimit(
            name = "ADMIN_REMOVE_ROLE",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> removeRole(
            @PathVariable Long userId,
            @PathVariable RoleName roleName
    ) {

        userRoleService.removeRole(userId, roleName);

        return ResponseData.success(
                null,
                "Remove role successfully",
                HttpStatus.OK
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    @Operation(summary = "Get roles of user")
    @RateLimit(
            name = "ADMIN_GET_USER_ROLES",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getUserRoles(
            @PathVariable Long userId
    ) {

        List<RoleResponse> response =
                userRoleService.getUserRoles(userId);

        return ResponseData.success(
                response,
                "Get user roles successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user roles")
    @RateLimit(
            name = "USER_GET_MY_ROLES",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMyRoles() {

        List<RoleResponse> response =
                userRoleService.getMyRoles();

        return ResponseData.success(
                response,
                "Get my roles successfully",
                HttpStatus.OK
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/check/{userId}/{roleName}")
    @Operation(summary = "Check if user has role")
    @RateLimit(
            name = "ADMIN_CHECK_ROLE",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> hasRole(
            @PathVariable Long userId,
            @PathVariable RoleName roleName
    ) {

        boolean result =
                userRoleService.hasRole(userId, roleName);

        return ResponseData.success(
                result,
                "Check role successfully",
                HttpStatus.OK
        );
    }
}