package media.social.modules.reel.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.reel.dto.request.CreateReelRequest;
import media.social.modules.reel.dto.request.UpdateReelContentRequest;
import media.social.modules.reel.dto.request.UpdateReelViewRequest;
import media.social.modules.reel.service.ReelService;
import media.social.modules.reel.service.ReelViewService;
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
    private final ReelViewService reelViewService;

    @GetMapping("/feed")
    @Operation(summary = "Get reel feed (friends + self)")
    @RateLimit(name = "REEL_FEED", limit = 120, windowSeconds = 60)
    public ResponseEntity<?> getReelFeed(Pageable pageable) {
        return ResponseData.successPaginate(reelService.getReelFeed(pageable), "Get reel feed successfully", HttpStatus.OK);
    }

    @GetMapping("/explore")
    @Operation(summary = "Get reel explore (public reels from strangers)")
    @RateLimit(name = "REEL_EXPLORE", limit = 60, windowSeconds = 60)
    public ResponseEntity<?> getReelExplore(Pageable pageable) {
        return ResponseData.successPaginate(reelService.getReelExplore(pageable), "Get reel explore successfully", HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get my reels")
    @RateLimit(name = "REEL_MY_LIST", limit = 120, windowSeconds = 60)
    public ResponseEntity<?> getMyReels(Pageable pageable) {
        return ResponseData.successPaginate(reelService.getMyReels(pageable), "Get my reels successfully", HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reels by user")
    @RateLimit(name = "REEL_USER_LIST", limit = 120, windowSeconds = 60)
    public ResponseEntity<?> getUserReels(@PathVariable Long userId, Pageable pageable) {
        return ResponseData.successPaginate(reelService.getUserReels(userId, pageable), "Get user reels successfully", HttpStatus.OK);
    }

    @GetMapping("/{reelId}")
    @Operation(summary = "Get reel detail")
    @RateLimit(name = "REEL_DETAIL", limit = 180, windowSeconds = 60)
    public ResponseEntity<?> getReelById(@PathVariable Long reelId) {
        return ResponseData.success(reelService.getReelById(reelId), "Get reel successfully", HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create new reel")
    @RateLimit(name = "REEL_CREATE", limit = 10, windowSeconds = 60)
    public ResponseEntity<?> createReel(@ModelAttribute @Valid CreateReelRequest request) {
        reelService.createReel(request);
        return ResponseData.success(null, "Create reel successfully", HttpStatus.CREATED);
    }

    @PatchMapping("/{reelId}")
    @Operation(summary = "Update reel content/visibility")
    @RateLimit(name = "REEL_UPDATE", limit = 40, windowSeconds = 60)
    public ResponseEntity<?> updateReel(@PathVariable Long reelId, @RequestBody @Valid UpdateReelContentRequest request) {
        reelService.updateReel(reelId, request);
        return ResponseData.success(null, "Update reel successfully", HttpStatus.OK);
    }

    @DeleteMapping("/{reelId}")
    @Operation(summary = "Delete reel")
    @RateLimit(name = "REEL_DELETE", limit = 20, windowSeconds = 60)
    public ResponseEntity<?> deleteReel(@PathVariable Long reelId) {
        reelService.deleteReel(reelId);
        return ResponseData.success(null, "Delete reel successfully", HttpStatus.OK);
    }

    // VIEW: Start View
    @PostMapping("/{reelId}/view")
    @Operation(summary = "Start view: tao record khi user mo Reel (idempotent)")
    @RateLimit(name = "REEL_VIEW_START", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> startView(@PathVariable Long reelId) {
        return ResponseData.success(reelViewService.startView(reelId), "View started", HttpStatus.OK);
    }

    // VIEW: Update Progress
    @PatchMapping("/{reelId}/view")
    @Operation(summary = "Update progress: cap nhat watchDurationMs va completed")
    @RateLimit(name = "REEL_VIEW_PROGRESS", limit = 600, windowSeconds = 60)
    public ResponseEntity<?> updateProgress(@PathVariable Long reelId, @RequestBody @Valid UpdateReelViewRequest request) {
        return ResponseData.success(reelViewService.updateProgress(reelId, request), "Progress updated", HttpStatus.OK);
    }

    // VIEW: Replay
    @PostMapping("/{reelId}/view/replay")
    @Operation(summary = "Replay: tang replayCount khi user xem lai Reel")
    @RateLimit(name = "REEL_VIEW_REPLAY", limit = 300, windowSeconds = 60)
    public ResponseEntity<?> replay(@PathVariable Long reelId) {
        return ResponseData.success(reelViewService.replay(reelId), "Replay counted", HttpStatus.OK);
    }

    @PostMapping("/{reelId}/share")
    @Operation(summary = "Increment reel share count")
    @RateLimit(name = "REEL_SHARE", limit = 60, windowSeconds = 60)
    public ResponseEntity<?> incrementShare(@PathVariable Long reelId) {
        reelService.incrementShareCount(reelId);
        return ResponseData.success(null, "Share counted", HttpStatus.OK);
    }
}