package media.social.modules.reel.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.reel.dto.request.CreateReelRequest;
import media.social.modules.reel.dto.request.UpdateReelContentRequest;
import media.social.modules.reel.service.ReelService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class ReelController {

    private final ReelService reelService;

    // ─── FEED ──────────────────────────────────────────────────────────────────

    @GetMapping("/feed")
    @Operation(summary = "Get reel feed (friends + self)")
    @RateLimit(
            name = "REEL_FEED",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getReelFeed(Pageable pageable) {

        return ResponseData.successPaginate(
                reelService.getReelFeed(pageable),
                "Get reel feed successfully",
                HttpStatus.OK
        );
    }

    // ─── EXPLORE ───────────────────────────────────────────────────────────────

    @GetMapping("/explore")
    @Operation(summary = "Get reel explore (public reels from strangers)")
    @RateLimit(
            name = "REEL_EXPLORE",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> getReelExplore(Pageable pageable) {

        return ResponseData.successPaginate(
                reelService.getReelExplore(pageable),
                "Get reel explore successfully",
                HttpStatus.OK
        );
    }

    // ─── MY REELS ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get my reels")
    @RateLimit(
            name = "REEL_MY_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMyReels(Pageable pageable) {

        return ResponseData.successPaginate(
                reelService.getMyReels(pageable),
                "Get my reels successfully",
                HttpStatus.OK
        );
    }

    // ─── USER REELS ────────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reels by user")
    @RateLimit(
            name = "REEL_USER_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getUserReels(
            @PathVariable Long userId,
            Pageable pageable
    ) {

        return ResponseData.successPaginate(
                reelService.getUserReels(userId, pageable),
                "Get user reels successfully",
                HttpStatus.OK
        );
    }

    // ─── REEL DETAIL ───────────────────────────────────────────────────────────

    @GetMapping("/{reelId}")
    @Operation(summary = "Get reel detail")
    @RateLimit(
            name = "REEL_DETAIL",
            limit = 180,
            windowSeconds = 60
    )
    public ResponseEntity<?> getReelById(@PathVariable Long reelId) {

        return ResponseData.success(
                reelService.getReelById(reelId),
                "Get reel successfully",
                HttpStatus.OK
        );
    }

    // ─── CREATE REEL ───────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create new reel")
    @RateLimit(
            name = "REEL_CREATE",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> createReel(
            @ModelAttribute @Valid CreateReelRequest request
    ) {

        reelService.createReel(request);

        return ResponseData.success(
                null,
                "Create reel successfully",
                HttpStatus.CREATED
        );
    }

    // ─── UPDATE REEL ───────────────────────────────────────────────────────────

    @PatchMapping("/{reelId}")
    @Operation(summary = "Update reel content/visibility")
    @RateLimit(
            name = "REEL_UPDATE",
            limit = 40,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateReel(
            @PathVariable Long reelId,
            @RequestBody @Valid UpdateReelContentRequest request
    ) {

        reelService.updateReel(reelId, request);

        return ResponseData.success(
                null,
                "Update reel successfully",
                HttpStatus.OK
        );
    }

    // ─── DELETE REEL ───────────────────────────────────────────────────────────

    @DeleteMapping("/{reelId}")
    @Operation(summary = "Delete reel")
    @RateLimit(
            name = "REEL_DELETE",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> deleteReel(@PathVariable Long reelId) {

        reelService.deleteReel(reelId);

        return ResponseData.success(
                null,
                "Delete reel successfully",
                HttpStatus.OK
        );
    }

    // ─── VIEW COUNT ────────────────────────────────────────────────────────────

    @PostMapping("/{reelId}/view")
    @Operation(summary = "Increment reel view count")
    @RateLimit(
            name = "REEL_VIEW",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> incrementView(@PathVariable Long reelId) {

        reelService.incrementViewCount(reelId);

        return ResponseData.success(
                null,
                "View counted",
                HttpStatus.OK
        );
    }

    // ─── SHARE COUNT ───────────────────────────────────────────────────────────

    @PostMapping("/{reelId}/share")
    @Operation(summary = "Increment reel share count")
    @RateLimit(
            name = "REEL_SHARE",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> incrementShare(@PathVariable Long reelId) {

        reelService.incrementShareCount(reelId);

        return ResponseData.success(
                null,
                "Share counted",
                HttpStatus.OK
        );
    }
}
