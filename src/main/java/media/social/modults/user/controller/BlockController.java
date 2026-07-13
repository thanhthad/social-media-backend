package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.user.dto.request.block.BlockRequest;
import media.social.modults.user.dto.response.block.BlockCheckResponse;
import media.social.modults.user.dto.response.block.ListUserBlockedResponse;
import media.social.modults.user.service.BlockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
@Tag(name = "Block Controller", description = "Block APIs")
public class BlockController {

    private final BlockService blockService;

    @PostMapping
    @Operation(summary = "Block a user")
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