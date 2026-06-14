package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.LikeRequest;
import media.social.modults.post.service.LikeService;
import media.social.modults.user.dto.response.common.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "Like Controller", description = "APIs for Like management")
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    @Operation(summary = "Like a post")
    public ResponseEntity<?> likePost(@RequestBody LikeRequest request) {

        likeService.likePost(request);

        return ResponseData.success(
                null,
                "Like post successfully",
                HttpStatus.CREATED
        );
    }

    @DeleteMapping
    @Operation(summary = "Unlike a post")
    public ResponseEntity<?> unlikePost(@RequestParam Long userId,
                                        @RequestParam Long postId) {

        likeService.unlikePost(userId, postId);

        return ResponseData.success(
                null,
                "Unlike post successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/count")
    @Operation(summary = "Count likes of a post")
    public ResponseEntity<?> countLikes(@RequestParam Long postId) {

        long count = likeService.countLikesByPost(postId);

        return ResponseData.success(
                count,
                "Count likes successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/check")
    @Operation(summary = "Check if user liked post")
    public ResponseEntity<?> isLiked(@RequestParam Long postId) {

        boolean check =  likeService.isPostLiked(postId);

        return ResponseData.success(
                check,
                "Check like status successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/posts/{postId}/users")
    @Operation(summary = "Get users who liked a post")
    public ResponseEntity<?> getUsersWhoLikedPost(@PathVariable Long postId,
                                                  Pageable pageable) {

        Page<UserResponse> page = likeService.getUsersWhoLikedPost(postId, pageable);

        return ResponseData.successPaginate(
                page,
                "Get users who liked post successfully",
                HttpStatus.OK
        );
    }
}