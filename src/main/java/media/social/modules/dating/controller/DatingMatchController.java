package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.service.DatingMatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating/matches")
@RequiredArgsConstructor
@Tag(name = "Dating Match Controller", description = "Dating Matches APIs")
public class DatingMatchController {

    private final DatingMatchService datingMatchService;

    @GetMapping
    @Operation(summary = "Get current user's dating matches")
    @RateLimit(name = "DATING_GET_MATCHES", limit = 120, windowSeconds = 60)
    public ResponseEntity<?> getMyMatches() {
        return ResponseData.success(
                datingMatchService.getMyMatches(),
                "Get matches successfully",
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{matchId}")
    @Operation(summary = "Unmatch a dating connection")
    @RateLimit(name = "DATING_UNMATCH", limit = 30, windowSeconds = 60)
    public ResponseEntity<?> unmatch(@PathVariable Long matchId) {
        datingMatchService.unmatch(matchId);
        return ResponseData.success(
                null,
                "Unmatch successfully",
                HttpStatus.OK
        );
    }
}
