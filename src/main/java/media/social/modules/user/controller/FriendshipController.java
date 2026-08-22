package media.social.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.user.dto.response.friend.FriendSuggestionResponse;
import media.social.modules.user.dto.response.friend.MutualFriendResponse;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import media.social.modules.user.service.FriendshipService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Tag(
        name = "Friendship Controller",
        description = "Friendship APIs"
)
public class FriendshipController {

    private final FriendshipService friendshipService;

    // ==================================================
    // FRIEND REQUEST
    // ==================================================

    @PostMapping("/{userId}")
    @Operation(summary = "Send a friend request")
    @RateLimit(
            name = "FRIEND_REQUEST_CREATE",
            limit = 50,
            windowSeconds = 60
    )
    public ResponseEntity<?> sendFriendRequest(
            @PathVariable Long userId
    ) {

        friendshipService.sendFriendRequest(userId);

        return ResponseData.success(
                null,
                "Friend request sent successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // ACCEPT FRIEND REQUEST
    // ==================================================

    @PostMapping("/{userId}/accept")
    @Operation(summary = "Accept a friend request")
    @RateLimit(
            name = "FRIEND_REQUEST_ACCEPT",
            limit = 50,
            windowSeconds = 60
    )
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable Long userId
    ) {

        friendshipService.acceptFriendRequest(userId);

        return ResponseData.success(
                null,
                "Friend request accepted successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // REJECT FRIEND REQUEST
    // ==================================================

    @PostMapping("/{userId}/reject")
    @Operation(summary = "Reject a friend request")
    @RateLimit(
            name = "FRIEND_REQUEST_REJECT",
            limit = 50,
            windowSeconds = 60
    )
    public ResponseEntity<?> rejectFriendRequest(
            @PathVariable Long userId
    ) {

        friendshipService.rejectFriendRequest(userId);

        return ResponseData.success(
                null,
                "Friend request rejected successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // CANCEL FRIEND REQUEST
    // ==================================================

    @DeleteMapping("/{userId}")
    @Operation(summary = "Cancel a sent friend request")
    @RateLimit(
            name = "FRIEND_REQUEST_CANCEL",
            limit = 50,
            windowSeconds = 60
    )
    public ResponseEntity<?> cancelFriendRequest(
            @PathVariable Long userId
    ) {

        friendshipService.cancelFriendRequest(userId);

        return ResponseData.success(
                null,
                "Friend request cancelled successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // GET PENDING FRIEND REQUESTS
    // ==================================================

    @GetMapping("/requests")
    @Operation(summary = "Get pending friend requests")
    @RateLimit(
            name = "FRIEND_REQUEST_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getPendingFriendRequests(
            Pageable pageable
    ) {

        Page<FriendshipUserResponse> page =
                friendshipService.getPendingFriendRequests(pageable);

        return ResponseData.successPaginate(
                page,
                "Get pending friend requests successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // GET FRIEND SUGGESTIONS
    // ==================================================

    @GetMapping("/suggestions")
    @Operation(summary = "Get friend suggestions based on mutual friends")
    @RateLimit(
            name = "FRIEND_SUGGESTIONS",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> getSuggestedUsers() {

        List<FriendSuggestionResponse> suggestions =
                friendshipService.getSuggestedUsers();

        return ResponseData.success(
                suggestions,
                "Get friend suggestions successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // GET MUTUAL FRIENDS
    // ==================================================

    @GetMapping("/{userId}/mutual-friends")
    @Operation(summary = "Get mutual friends with a user")
    @RateLimit(
            name = "MUTUAL_FRIEND_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMutualFriends(
            @PathVariable Long userId
    ) {

        List<MutualFriendResponse> mutualFriends =
                friendshipService.getMutualFriends(userId);

        return ResponseData.success(
                mutualFriends,
                "Get mutual friends successfully",
                HttpStatus.OK
        );
    }

    // ==================================================
    // GET FRIENDS OF USER
    // ==================================================

    @GetMapping("/{userId}")
    @Operation(summary = "Get friends of a user")
    @RateLimit(
            name = "FRIEND_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getFriends(
            @PathVariable Long userId,
            Pageable pageable
    ) {

        Page<FriendshipUserResponse> page =
                friendshipService.getFriends(userId, pageable);

        return ResponseData.successPaginate(
                page,
                "Get friends successfully",
                HttpStatus.OK
        );
    }

}