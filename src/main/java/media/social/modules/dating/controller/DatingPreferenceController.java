package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.dto.request.preference.UpdateDatingPreferenceRequest;
import media.social.modules.dating.service.DatingPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating")
@RequiredArgsConstructor
@Tag(
        name = "Dating Preference Controller",
        description = "Dating preference APIs"
)
public class DatingPreferenceController {

    private final DatingPreferenceService datingPreferenceService;

    @PostMapping("/me/preferences")
    @Operation(summary = "Create dating preference")
    @RateLimit(name = "DATING_CREATE_PREFERENCE", limit = 5, windowSeconds = 60)
    public ResponseEntity<?> createPreference(
            @Valid @RequestBody UpdateDatingPreferenceRequest request
    ) {
        return ResponseData.success(
                datingPreferenceService.createPreference(request),
                "Create dating preference successfully",
                HttpStatus.CREATED
        );
    }

    @GetMapping("/me/preferences")
    @Operation(summary = "Get my dating preference")
    @RateLimit(name = "DATING_GET_PREFERENCE", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getMyPreference() {
        return ResponseData.success(
                datingPreferenceService.getMyPreference(),
                "Get dating preference successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update dating preference")
    @RateLimit(name = "DATING_UPDATE_PREFERENCE", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updatePreference(
            @Valid @RequestBody UpdateDatingPreferenceRequest request
    ) {
        return ResponseData.success(
                datingPreferenceService.updatePreference(request),
                "Update dating preference successfully",
                HttpStatus.OK
        );
    }
}