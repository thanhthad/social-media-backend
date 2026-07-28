package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.user.dto.request.user.ChangePasswordRequest;
import media.social.modults.user.dto.request.user.UpdateAvatarRequest;
import media.social.modults.user.dto.request.user.UpdateProfileRequest;
import media.social.modults.user.dto.response.user.ProfileResponse;
import media.social.modults.user.dto.response.user.PublicUserProfileResponse;
import media.social.modults.user.dto.response.user.UserSearchResponse;
import media.social.modults.user.dto.response.user.UserProfileResponse;
import media.social.modults.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "User APIs")
public class UserController {

    private final UserService userService;

    // ==================================================
    // SELF APIs
    // ==================================================

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<?> getMe() {

        UserProfileResponse response = userService.getMe();

        return ResponseData.success(
                response,
                "Get profile successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<?> updateMe(
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        ProfileResponse profileResponse =  userService.updateMe(request);

        return ResponseData.success(
                profileResponse,
                "Update profile successfully",
                HttpStatus.OK
        );
    }

    @PostMapping(value = "me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(
            @Valid @ModelAttribute UpdateAvatarRequest request
    ) {
        ProfileResponse profileResponse =  userService.updateAvatar(request);

        return ResponseData.success(
                profileResponse,
                "Update avatar successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password")
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
    public ResponseEntity<?> getUserById(
            @PathVariable Long id
    ) {

        PublicUserProfileResponse response =
                userService.getUserById(id);

        return ResponseData.success(
                response,
                "Get user successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username")
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