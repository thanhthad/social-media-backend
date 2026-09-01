package media.social.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.user.dto.request.profile.*;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.user.*;
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

    // ================= SELF PROFILE =================

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @RateLimit(name = "USER_GET_ME", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getMe() {
        return ResponseData.success(
                userService.getMe(),
                "Get profile successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/basic")
    @Operation(summary = "Update basic profile")
    @RateLimit(name = "USER_UPDATE_BASIC_PROFILE", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateBasicProfile(
            @Valid @RequestBody UpdateBasicProfileRequest request
    ) {
        return ResponseData.success(
                userService.updateBasicProfile(request),
                "Update basic profile successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/contact")
    @Operation(summary = "Update contact information")
    @RateLimit(name = "USER_UPDATE_CONTACT", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateContact(
            @Valid @RequestBody UpdateContactRequest request
    ) {
        return ResponseData.success(
                userService.updateContact(request),
                "Update contact successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/career")
    @Operation(summary = "Update career information")
    @RateLimit(name = "USER_UPDATE_CAREER", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateCareer(
            @Valid @RequestBody UpdateCareerRequest request
    ) {
        return ResponseData.success(
                userService.updateCareer(request),
                "Update career successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/social-links")
    @Operation(summary = "Update social links")
    @RateLimit(name = "USER_UPDATE_SOCIAL_LINKS", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateSocialLinks(
            @Valid @RequestBody UpdateSocialLinksRequest request
    ) {
        return ResponseData.success(
                userService.updateSocialLinks(request),
                "Update social links successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/visibility")
    @Operation(summary = "Update profile visibility")
    @RateLimit(name = "USER_UPDATE_VISIBILITY", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateVisibility(
            @Valid @RequestBody UpdateProfileVisibilityRequest request
    ) {
        return ResponseData.success(
                userService.updateProfileVisibility(request),
                "Update profile visibility successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/username")
    @Operation(summary = "Update username")
    @RateLimit(name = "USER_UPDATE_USERNAME", limit = 10, windowSeconds = 60)
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
    @RateLimit(name = "USER_UPLOAD_AVATAR", limit = 10, windowSeconds = 60)
    public ResponseEntity<?> updateAvatar(
            @Valid @ModelAttribute UpdateAvatarRequest request
    ) {
        return ResponseData.success(
                userService.updateAvatar(request),
                "Update avatar successfully",
                HttpStatus.OK
        );
    }

    @PostMapping(
            value = "/me/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Update cover image")
    @RateLimit(name = "USER_UPLOAD_COVER", limit = 10, windowSeconds = 60)
    public ResponseEntity<?> updateCover(
            @Valid @ModelAttribute UpdateCoverRequest request
    ) {
        return ResponseData.success(
                userService.updateCover(request),
                "Update cover successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password")
    @RateLimit(name = "USER_CHANGE_PASSWORD", limit = 5, windowSeconds = 60)
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


    @GetMapping("/{id}")
    @Operation(summary = "Get public profile by user id")
    @RateLimit(name = "USER_PUBLIC_PROFILE", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getUserById(
            @PathVariable Long id
    ) {
        return ResponseData.success(
                userService.getUserById(id),
                "Get user successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get user statistics and friendship status")
    @RateLimit(name = "USER_STATS", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getUserStats(
            @PathVariable Long id
    ) {
        return ResponseData.success(
                userService.getUserStats(id),
                "Get user stats successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username")
    @RateLimit(name = "USER_SEARCH", limit = 60, windowSeconds = 60)
    public ResponseEntity<?> searchUsers(
            @RequestParam String username,
            Pageable pageable
    ) {
        Page<UserSearchResponse> page =
                userService.findUsersByName(username, pageable);

        return ResponseData.successPaginate(
                page,
                "Search users successfully",
                HttpStatus.OK
        );
    }
}