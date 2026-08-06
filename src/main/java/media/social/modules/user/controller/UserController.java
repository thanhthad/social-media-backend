package media.social.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.user.ProfileResponse;
import media.social.modules.user.dto.response.user.UserProfileResponse;
import media.social.modules.user.dto.response.user.UserSearchResponse;
import media.social.modules.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(
        name = "User Controller",
        description = "User APIs"
)
public class UserController {

    private final UserService userService;

    // ==================================================
    // SELF APIs
    // ==================================================
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @RateLimit(
            name = "USER_GET_ME",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMe() {

        return ResponseData.success(
                userService.getMe(),
                "Get profile successfully",
                HttpStatus.OK
        );
    }


    @PutMapping("/me/username")
    @Operation(summary = "Update username")
    @RateLimit(
            name = "USER_UPDATE_USERNAME",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateUsername(
            @Valid @RequestBody UpdateUsernameRequest request
    ) {

        UserProfileResponse response =
                userService.updateUserName(request);


        return ResponseData.success(
                response,
                "Update username successfully",
                HttpStatus.OK
        );
    }

    @PostMapping(
            value = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Update avatar")
    @RateLimit(
            name = "USER_UPLOAD_AVATAR",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateAvatar(
            @Valid @ModelAttribute UpdateAvatarRequest request
    ) {

        ProfileResponse profileResponse =
                userService.updateAvatar(request);

        return ResponseData.success(
                profileResponse,
                "Update avatar successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password")
    @RateLimit(
            name = "USER_CHANGE_PASSWORD",
            limit = 5,
            windowSeconds = 60
    )
    public ResponseEntity<?> updatePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.updatePassword(request);
        return ResponseData.success(
                null,
                "Password updated successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // PUBLIC APIs
    // ==================================================
    @GetMapping("/{id}")
    @Operation(summary = "Get public profile by user id")
    @RateLimit(
            name = "USER_PUBLIC_PROFILE",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> getUserById(
            @PathVariable Long id
    ) {

        return ResponseData.success(
                userService.getUserById(id),
                "Get user successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username")
    @RateLimit(
            name = "USER_SEARCH",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> searchUsers(
            @RequestParam String username,
            Pageable pageable
    ) {

        Page<UserSearchResponse> page =
                userService.findUsersByName(
                        username,
                        pageable
                );

        return ResponseData.successPaginate(
                page,
                "Search users successfully",
                HttpStatus.OK
        );
    }
}