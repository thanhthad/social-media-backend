package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.dto.request.profile.*;
import media.social.modules.dating.service.DatingProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating")
@RequiredArgsConstructor
@Tag(
        name = "Dating Profile Controller",
        description = "Dating profile APIs"
)
public class DatingProfileController {

    private final DatingProfileService datingProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get my dating profile")
    @RateLimit(name = "DATING_GET_ME", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getMe() {
        return ResponseData.success(
                datingProfileService.getMe(),
                "Get dating profile successfully",
                HttpStatus.OK
        );
    }

    @PostMapping("/me")
    @Operation(summary = "Create dating profile")
    @RateLimit(name = "DATING_CREATE_PROFILE", limit = 5, windowSeconds = 60)
    public ResponseEntity<?> createProfile(
            @Valid @RequestBody CreateDatingProfileRequest request
    ) {
        return ResponseData.success(
                datingProfileService.createProfile(request),
                "Create dating profile successfully",
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/profile/coordinates")
    @Operation(summary = "Update my dating coordinates")
    @RateLimit(
            name = "DATING_UPDATE_COORDINATES",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateCoordinates(
            @Valid @RequestBody UpdateDatingCoordinatesRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateCoordinates(request),
                "Update dating coordinates successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/basic")
    @Operation(summary = "Update dating basic information")
    @RateLimit(name = "DATING_UPDATE_BASIC", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateBasicInfo(
            @Valid @RequestBody UpdateDatingBasicInfoRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateBasicInfo(request),
                "Update basic information successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/career")
    @Operation(summary = "Update dating career information")
    @RateLimit(name = "DATING_UPDATE_CAREER", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateCareer(
            @Valid @RequestBody UpdateDatingCareerRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateCareer(request),
                "Update career successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/location")
    @Operation(summary = "Update dating location")
    @RateLimit(name = "DATING_UPDATE_LOCATION", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateLocation(
            @Valid @RequestBody UpdateDatingLocationRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateLocation(request),
                "Update location successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/bio")
    @Operation(summary = "Update dating bio")
    @RateLimit(name = "DATING_UPDATE_BIO", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateBio(
            @Valid @RequestBody UpdateDatingBioRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateBio(request),
                "Update bio successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/profile/visibility")
    @Operation(summary = "Update dating visibility")
    @RateLimit(name = "DATING_UPDATE_VISIBILITY", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateVisibility(
            @Valid @RequestBody UpdateDatingVisibilityRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateVisibility(request),
                "Update visibility successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/status")
    @Operation(summary = "Update dating status")
    @RateLimit(name = "DATING_UPDATE_STATUS", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateStatus(
            @Valid @RequestBody UpdateDatingStatusRequest request
    ) {
        return ResponseData.success(
                datingProfileService.updateStatus(request),
                "Update dating status successfully",
                HttpStatus.OK
        );
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete dating profile")
    @RateLimit(name = "DATING_DELETE_PROFILE", limit = 5, windowSeconds = 60)
    public ResponseEntity<?> deleteProfile() {
        datingProfileService.deleteProfile();
        return ResponseData.success(
                null,
                "Delete dating profile successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get public dating profile")
    @RateLimit(name = "DATING_PUBLIC_PROFILE", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getPublicProfile(
            @PathVariable Long userId
    ) {
        return ResponseData.success(
                datingProfileService.getPublicProfile(userId),
                "Get public dating profile successfully",
                HttpStatus.OK
        );
    }
}