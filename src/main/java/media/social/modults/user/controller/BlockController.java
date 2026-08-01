package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modults.user.dto.request.block.BlockRequest;
import media.social.modults.user.dto.response.block.ListUserBlockedResponse;
import media.social.modults.user.service.BlockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
@Tag(name = "Block Controller", description = "Block APIs")
public class BlockController {

    private final BlockService blockService;

    @PostMapping
    @Operation(summary = "Block a user")
    @RateLimit(
            name = "BLOCK_CREATE",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> blockUser(
            @Valid @RequestBody BlockRequest request
    ) {

        blockService.blockUser(request);

        return ResponseData.success(
                null,
                "Block user successfully",
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/{blockedId}")
    @Operation(summary = "Unblock a user")
    @RateLimit(
            name = "BLOCK_DELETE",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> unblockUser(
            @PathVariable Long blockedId
    ) {

        blockService.unblockUser(blockedId);

        return ResponseData.success(
                null,
                "Unblock user successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/check/{userId}")
    @Operation(summary = "Check whether current user blocked target user")
    @RateLimit(
            name = "BLOCK_CHECK",
            limit = 500,
            windowSeconds = 60
    )
    public ResponseEntity<?> checkBlocked(
            @PathVariable Long userId
    ) {

        return ResponseData.success(
                blockService.checkBlocked(userId),
                "Check block status successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get blocked users of current user")
    @RateLimit(
            name = "BLOCK_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getBlockedUsers(
            Pageable pageable
    ) {

        Page<ListUserBlockedResponse> response =
                blockService.getBlockedUsers(pageable);

        return ResponseData.success(
                response,
                "Get blocked users successfully",
                HttpStatus.OK
        );
    }
}