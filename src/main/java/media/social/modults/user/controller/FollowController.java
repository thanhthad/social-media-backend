package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
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
    @RateLimit(
            name = "FOLLOW_CREATE",
            limit = 50,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "FOLLOW_DELETE",
            limit = 50,
            windowSeconds = 60
    )
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
    // LIST FOLLOWERS / FOLLOWING (PUBLIC)
    // ==================================================

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get followers of a user")
    @RateLimit(
            name = "FOLLOWER_LIST",
            limit = 120,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "FOLLOWING_LIST",
            limit = 120,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "MY_FOLLOWER_LIST",
            limit = 120,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "MY_FOLLOWING_LIST",
            limit = 120,
            windowSeconds = 60
    )
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