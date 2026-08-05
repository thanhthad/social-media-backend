package media.social.modules.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.post.dto.request.post.CreatePostRequest;
import media.social.modules.post.dto.request.post.UpdatePostContent;
import media.social.modules.post.dto.request.post.UpdatePostMedia;
import media.social.modules.post.service.PostService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // ================= FEED =================
    @GetMapping("/feed")
    @Operation(summary = "Get news feed")
    @RateLimit(
            name = "POST_FEED",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getFeed(Pageable pageable) {

        return ResponseData.success(
                postService.getFeed(pageable),
                "Get feed successfully",
                HttpStatus.OK
        );
    }

    // ================= EXPLORE =================
    @GetMapping("/explore")
    @Operation(summary = "Get explore feed")
    @RateLimit(
            name = "POST_EXPLORE",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> getExplore(Pageable pageable) {

        return ResponseData.success(
                postService.getExplore(pageable),
                "Get explore successfully",
                HttpStatus.OK
        );
    }

    // ================= SEARCH BY CONTENT =================
    @GetMapping("/search")
    @Operation(summary = "Search posts by content")
    @RateLimit(
            name = "POST_SEARCH",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> searchByContent(
            @RequestParam String keyword,
            Pageable pageable
    ) {

        return ResponseData.success(
                postService.searchByContent(keyword, pageable),
                "Search successfully",
                HttpStatus.OK
        );
    }

    // ================= SEARCH BY HASHTAG =================
    @GetMapping("/search/hashtag")
    @Operation(summary = "Search posts by hashtag")
    @RateLimit(
            name = "POST_SEARCH_HASHTAG",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> searchByHashtag(
            @RequestParam String name,
            Pageable pageable
    ) {

        return ResponseData.success(
                postService.searchByHashtag(name, pageable),
                "Search hashtag successfully",
                HttpStatus.OK
        );
    }

    // ================= CREATE POST =================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create new post")
    @RateLimit(
            name = "POST_CREATE",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> createPost(
            @ModelAttribute @Valid CreatePostRequest request
    ) {

        postService.createPost(request);

        return ResponseData.success(
                null,
                "Create post successfully",
                HttpStatus.CREATED
        );
    }

    // ================= UPDATE POST =================
    @PatchMapping("/{postId}")
    @Operation(summary = "Update post content")
    @RateLimit(
            name = "POST_UPDATE",
            limit = 40,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateContent(
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostContent request
    ) {

        postService.updatePostContent(postId, request);

        return ResponseData.success(
                null,
                "Update post successfully",
                HttpStatus.OK
        );
    }

    // ================= ADD MEDIA =================
    @PostMapping(
            value = "/{postId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Upload post media")
    @RateLimit(
            name = "POST_UPLOAD_MEDIA",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> addMedia(
            @PathVariable Long postId,
            @ModelAttribute @Valid UpdatePostMedia request
    ) {

        postService.updatePostMedia(postId, request);

        return ResponseData.success(
                null,
                "Upload media successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE POST =================
    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post")
    @RateLimit(
            name = "POST_DELETE",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId
    ) {

        postService.deleteByPostId(postId);

        return ResponseData.success(
                null,
                "Delete post successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE MEDIA =================
    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete media")
    @RateLimit(
            name = "POST_DELETE_MEDIA",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> deleteMedia(
            @PathVariable Long mediaId
    ) {

        postService.deletePostMedia(mediaId);

        return ResponseData.success(
                null,
                "Delete media successfully",
                HttpStatus.OK
        );
    }

    // ================= MY POSTS =================
    @GetMapping
    @Operation(summary = "Get my posts")
    @RateLimit(
            name = "POST_MY_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getAll(Pageable pageable) {

        return ResponseData.success(
                postService.getAllPostMe(pageable),
                "Get posts successfully",
                HttpStatus.OK
        );
    }

    // ================= POST DETAIL =================
    @GetMapping("/{postId}")
    @Operation(summary = "Get post detail")
    @RateLimit(
            name = "POST_DETAIL",
            limit = 180,
            windowSeconds = 60
    )
    public ResponseEntity<?> getPostById(
            @PathVariable Long postId
    ) {

        return ResponseData.success(
                postService.getPostById(postId),
                "Get post successfully",
                HttpStatus.OK
        );
    }

    // ================= USER POSTS =================
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user")
    @RateLimit(
            name = "POST_USER_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getPostsByUserId(
            @PathVariable Long userId,
            Pageable pageable
    ) {

        return ResponseData.success(
                postService.getAllPostByUserId(userId, pageable),
                "Get user posts successfully",
                HttpStatus.OK
        );
    }

    // ================= SAVED POSTS =================
    @GetMapping("/savedPost")
    @Operation(summary = "Get saved posts")
    @RateLimit(
            name = "POST_SAVED_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getSavedPosts(
            Pageable pageable
    ) {

        return ResponseData.success(
                postService.getAllSavedPost(pageable),
                "Get saved posts successfully",
                HttpStatus.OK
        );
    }
}