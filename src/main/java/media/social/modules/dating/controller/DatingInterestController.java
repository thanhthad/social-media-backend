package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.dto.request.interest.UpdateDatingInterestRequest;
import media.social.modules.dating.service.DatingInterestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating")
@RequiredArgsConstructor
@Tag(
        name = "Dating Interest Controller",
        description = "Dating interest APIs"
)
public class DatingInterestController {

    private final DatingInterestService datingInterestService;

    @GetMapping("/interests")
    @Operation(summary = "Get all dating interests")
    @RateLimit(name = "DATING_GET_INTERESTS", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getAllInterest() {
        return ResponseData.success(
                datingInterestService.getAllInterest(),
                "Get dating interests successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/me/interests")
    @Operation(summary = "Get my dating interests")
    @RateLimit(name = "DATING_GET_MY_INTERESTS", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> getMyInterest() {
        return ResponseData.success(
                datingInterestService.getMyInterest(),
                "Get my dating interests successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/me/interests")
    @Operation(summary = "Update my dating interests")
    @RateLimit(name = "DATING_UPDATE_INTERESTS", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> updateMyInterest(
            @Valid @RequestBody UpdateDatingInterestRequest request
    ) {
        return ResponseData.success(
                datingInterestService.updateMyInterest(request),
                "Update dating interests successfully",
                HttpStatus.OK
        );
    }
}