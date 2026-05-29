package media.social.modults.others.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.others.dto.request.CreatePostRequest;
import media.social.modults.others.dto.request.UpdatePostRequest;
import media.social.modults.others.dto.response.PostResponse;
import media.social.modults.others.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Post Controller", description = "APIs for Post management")
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create post")
    public ResponseEntity<?> createPost(@ModelAttribute @Valid CreatePostRequest request
    ) {
        PostResponse response = postService.createPost(request);
        return ResponseData.success(response, "Create post successfully", HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update post")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @ModelAttribute @Valid UpdatePostRequest request){
        PostResponse response = postService.updatePost(id, request);
        return ResponseData.success(response, "Update post successfully", HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post by id")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        postService.deleteByPostId(id);
        return ResponseData.success(null, "Delete post successfully", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by id")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        PostResponse response = postService.getPostById(id);
        return ResponseData.success(response, "Get post successfully", HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get all posts with pagination")
    public ResponseEntity<?> getAllPosts(Pageable pageable) {
        Page<PostResponse> page = postService.getAllPosts(pageable);
        return ResponseData.successPaginate(page, "Get all posts successfully", HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all posts by userId")
    public ResponseEntity<?> getPostsByUserId(@PathVariable Long userId,
                                              Pageable pageable) {
        Page<PostResponse> page = postService.getAllPostsByUserId(userId, pageable);
        return ResponseData.successPaginate(page, "Get posts by user successfully", HttpStatus.OK);
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete all posts by userId")
    public ResponseEntity<?> deletePostsByUserId(@PathVariable Long userId) {
        postService.deleteByUserId(userId);
        return ResponseData.success(null, "Delete posts by user successfully", HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search posts by content keyword")
    public ResponseEntity<?> searchByContent(@RequestParam String keyword,
                                             Pageable pageable) {
        Page<PostResponse> page = postService.getByContent(keyword, pageable);
        return ResponseData.successPaginate(page, "Search posts successfully", HttpStatus.OK);
    }

    @GetMapping("/filter/date")
    @Operation(summary = "Get posts by created date range")
    public ResponseEntity<?> getByCreatedAtBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable
    ) {
        Page<PostResponse> page = postService.getByCreatedAtBetween(start, end, pageable);
        return ResponseData.successPaginate(page, "Filter posts by date successfully", HttpStatus.OK);
    }
}