package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.user.dto.response.user.FollowCountResponse;
import media.social.modults.user.dto.response.user.FollowUserResponse;
import media.social.modults.user.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@Tag(name = "Follow Controller", description = "Follow APIs")
public class FollowController {

    private final FollowService followService;

    // ==================================================
    // FOLLOW / UNFOLLOW
    // ==================================================

    @PostMapping("/{userId}")
    @Operation(summary = "Follow a user")
    public ResponseEntity<?> followUser(
            @PathVariable Long userId
    ) {

        followService.followUser(userId);

        return ResponseData.success(
                null,
                "Follow user successfully",
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<?> unfollowUser(
            @PathVariable Long userId
    ) {

        followService.unfollowUser(userId);

        return ResponseData.success(
                null,
                "Unfollow user successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // STATUS
    // ==================================================

    @GetMapping("/{userId}/status")
    @Operation(summary = "Check if current user is following")
    public ResponseEntity<?> isFollowing(
            @PathVariable Long userId
    ) {

        boolean result = followService.isFollowing(userId);

        return ResponseData.success(
                result,
                "Check follow status successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // COUNT
    // ==================================================

    @GetMapping("/{userId}/count")
    @Operation(summary = "Get follow counts of a user")
    public ResponseEntity<?> getFollowCount(
            @PathVariable Long userId
    ) {

        FollowCountResponse response =
                followService.getProfile(userId);

        return ResponseData.success(
                response,
                "Get follow count successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/me/count")
    @Operation(summary = "Get my follow counts")
    public ResponseEntity<?> getMyFollowCount() {

        FollowCountResponse response =
                followService.geMytProfile();

        return ResponseData.success(
                response,
                "Get my follow count successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // LIST FOLLOWERS / FOLLOWING (PUBLIC)
    // ==================================================

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get followers of a user")
    public ResponseEntity<?> getFollowers(
            @PathVariable Long userId,
            Pageable pageable
    ) {

        Page<FollowUserResponse> page =
                followService.getFollowers(userId, pageable);

        return ResponseData.successPaginate(
                page,
                "Get followers successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "Get following of a user")
    public ResponseEntity<?> getFollowing(
            @PathVariable Long userId,
            Pageable pageable
    ) {

        Page<FollowUserResponse> page =
                followService.getFollowing(userId, pageable);

        return ResponseData.successPaginate(
                page,
                "Get following successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // LIST FOLLOWERS / FOLLOWING (SELF)
    // ==================================================

    @GetMapping("/me/followers")
    @Operation(summary = "Get my followers")
    public ResponseEntity<?> getMyFollowers(
            Pageable pageable
    ) {

        Page<FollowUserResponse> page =
                followService.getMyFollowers(pageable);

        return ResponseData.successPaginate(
                page,
                "Get my followers successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/me/following")
    @Operation(summary = "Get my following")
    public ResponseEntity<?> getMyFollowing(
            Pageable pageable
    ) {

        Page<FollowUserResponse> page =
                followService.getMyFollowing(pageable);

        return ResponseData.successPaginate(
                page,
                "Get my following successfully",
                HttpStatus.OK
        );
    }
}