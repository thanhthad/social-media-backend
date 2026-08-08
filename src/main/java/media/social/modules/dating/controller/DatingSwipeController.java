package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.dto.request.swipe.CreateDatingSwipeRequest;
import media.social.modules.dating.service.DatingSwipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating")
@RequiredArgsConstructor
@Tag(
        name = "Dating Swipe Controller",
        description = "Dating swipe APIs"
)
public class DatingSwipeController {

    private final DatingSwipeService datingSwipeService;

    @PostMapping("/swipes")
    @Operation(summary = "Swipe a dating user")
    @RateLimit(name = "DATING_SWIPE", limit = 60, windowSeconds = 60)
    public ResponseEntity<?> swipe(
            @Valid @RequestBody CreateDatingSwipeRequest request
    ) {
        return ResponseData.success(
                datingSwipeService.swipe(request),
                "Swipe successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/swipes/me")
    @Operation(summary = "Get my dating swipes")
    @RateLimit(name = "DATING_GET_MY_SWIPES", limit = 100, windowSeconds = 60)
    public ResponseEntity<?> getMySwipes() {
        return ResponseData.success(
                datingSwipeService.getMySwipes(),
                "Get my swipes successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/swipes/me/likes")
    @Operation(summary = "Get users I liked")
    @RateLimit(name = "DATING_GET_MY_LIKES", limit = 100, windowSeconds = 60)
    public ResponseEntity<?> getMyLikes() {
        return ResponseData.success(
                datingSwipeService.getMyLikes(),
                "Get my likes successfully",
                HttpStatus.OK
        );
    }

    @DeleteMapping("/swipes/{targetUserId}")
    @Operation(summary = "Remove my swipe")
    @RateLimit(name = "DATING_DELETE_SWIPE", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> deleteSwipe(
            @PathVariable
            @Positive(message = "Target user ID must be positive")
            Long targetUserId
    ) {
        datingSwipeService.deleteSwipe(targetUserId);

        return ResponseData.success(
                null,
                "Delete swipe successfully",
                HttpStatus.OK
        );
    }
}