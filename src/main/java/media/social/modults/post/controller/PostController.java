package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.CreatePostRequest;
import media.social.modults.post.dto.request.UpdatePostContent;
import media.social.modults.post.dto.request.UpdatePostMedia;
import media.social.modults.post.service.PostService;
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

    // ================= CREATE POST =================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create new post with media")
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

    // ================= UPDATE CONTENT =================
    @PatchMapping("/{postId}")
    @Operation(summary = "Update post content")
    public ResponseEntity<?> updateContent(
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostContent request
    ) {
        postService.updatePostContent(postId, request);

        return ResponseData.success(
                null,
                "Update post content successfully",
                HttpStatus.OK
        );
    }

    // ================= ADD MEDIA =================
    @PostMapping(
            value = "/{postId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Add media to post")
    public ResponseEntity<?> addMedia(
            @PathVariable Long postId,
            @ModelAttribute @Valid UpdatePostMedia request
    ) {
        postService.updatePostMedia(postId, request);

        return ResponseData.success(
                null,
                "Add media successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE POST =================
    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post by id")
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

    // ================= DELETE MEDIA (FIXED) =================
    @DeleteMapping("/media")
    @Operation(summary = "Delete media by publicId")
    public ResponseEntity<?> deleteMedia(
            @RequestParam String publicId
    ) {
        postService.deletePostMedia(publicId);

        return ResponseData.success(
                null,
                "Delete media successfully",
                HttpStatus.OK
        );
    }

    // ================= GET FEED =================
    @GetMapping
    @Operation(summary = "Get feed posts (current user)")
    public ResponseEntity<?> getAll(Pageable pageable) {

        return ResponseData.success(
                postService.getAllPostMe(pageable),
                "Get posts successfully",
                HttpStatus.OK
        );
    }

    // ================= GET USER POSTS =================
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by userId")
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
}