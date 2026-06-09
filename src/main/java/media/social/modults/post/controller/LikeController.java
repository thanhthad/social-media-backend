package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.LikeRequest;
import media.social.modults.post.dto.response.LikeResponse;
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

    /*
     =========================================================
     1. LIKE POST
     =========================================================
     */
    @PostMapping
    @Operation(summary = "Like a post")
    public ResponseEntity<?> likePost(@RequestBody LikeRequest request) {

        LikeResponse response = likeService.likePost(request);

        return ResponseData.success(
                response,
                "Like post successfully",
                HttpStatus.CREATED
        );
    }

    /*
     =========================================================
     2. UNLIKE POST
     =========================================================
     */
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

    /*
     =========================================================
     3. COUNT LIKES
     =========================================================
     */
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

    /*
     =========================================================
     4. CHECK USER LIKED POST
     =========================================================
     */
    @GetMapping("/check")
    @Operation(summary = "Check if user liked post")
    public ResponseEntity<?> isLiked(@RequestParam Long userId,
                                     @RequestParam Long postId) {

        boolean result = likeService.isPostLiked(userId, postId);

        return ResponseData.success(
                result,
                "Check like status successfully",
                HttpStatus.OK
        );
    }

    /*
     =========================================================
     5. USERS WHO LIKED POST
     =========================================================
     */
    @GetMapping("/post/{postId}/users")
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

    /*
     =========================================================
     6. POSTS LIKED BY USER
     =========================================================
     */
//    @GetMapping("/user/{userId}/posts")
//    @Operation(summary = "Get posts liked by user")
//    public ResponseEntity<?> getPostsLikedByUser(@PathVariable Long userId,
//                                                 Pageable pageable) {
//
//        Page<PostResponse> page = likeService.getPostsLikedByUser(userId, pageable);
//
//        return ResponseData.successPaginate(
//                page,
//                "Get posts liked by user successfully",
//                HttpStatus.OK
//        );
//    }

    /*
     =========================================================
     7. TOP LIKED POSTS
     =========================================================
     */
    @GetMapping("/top-posts")
    @Operation(summary = "Get top liked posts")
    public ResponseEntity<?> getTopLikedPosts(Pageable pageable) {

        Page<Object[]> page = likeService.getTopLikedPosts(pageable);

        return ResponseData.successPaginate(
                page,
                "Get top liked posts successfully",
                HttpStatus.OK
        );
    }
}