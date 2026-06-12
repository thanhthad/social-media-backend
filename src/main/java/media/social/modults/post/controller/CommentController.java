package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.CreateCommentRequest;
import media.social.modults.post.dto.request.ReplyCommentRequest;
import media.social.modults.post.dto.request.UpdateCommentContent;
import media.social.modults.post.service.CommentService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ================= CREATE COMMENT =================
    @PostMapping
    @Operation(summary = "Create comment (root or reply)")
    public ResponseEntity<?> createComment(
            @RequestBody @Valid CreateCommentRequest request
    ) {
        return ResponseData.success(
                commentService.createComment(request),
                "Create comment successfully",
                HttpStatus.CREATED
        );
    }

    // ================= REPLY COMMENT =================
    @PostMapping("/reply")
    @Operation(summary = "Reply to a comment")
    public ResponseEntity<?> replyComment(
            @RequestBody @Valid ReplyCommentRequest request
    ) {
        return ResponseData.success(
                commentService.replyComment(request),
                "Reply comment successfully",
                HttpStatus.CREATED
        );
    }

    // ================= UPDATE COMMENT =================
    @PatchMapping("/{commentId}")
    @Operation(summary = "Update comment content")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentContent request
    ) {
        return ResponseData.success(
                commentService.updateComment(commentId, request),
                "Update comment successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE COMMENT =================
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);

        return ResponseData.success(
                null,
                "Delete comment successfully",
                HttpStatus.OK
        );
    }

    // ================= GET ROOT COMMENTS =================
    @GetMapping("/post/{postId}")
    @Operation(summary = "Get root comments of a post")
    public ResponseEntity<?> getRootComments(
            @PathVariable Long postId,
            Pageable pageable
    ) {
        return ResponseData.success(
                commentService.getRootComments(postId, pageable),
                "Get root comments successfully",
                HttpStatus.OK
        );
    }

    // ================= GET REPLIES =================
    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies of a comment")
    public ResponseEntity<?> getReplies(
            @PathVariable Long commentId,
            Pageable pageable
    ) {
        return ResponseData.success(
                commentService.getReplies(commentId, pageable),
                "Get replies successfully",
                HttpStatus.OK
        );
    }

    // ================= COUNT COMMENTS =================
    @GetMapping("/post/{postId}/count")
    @Operation(summary = "Count total comments of a post")
    public ResponseEntity<?> countComments(
            @PathVariable Long postId
    ) {
        return ResponseData.success(
                commentService.countCommentsByPost(postId),
                "Count comments successfully",
                HttpStatus.OK
        );
    }
}