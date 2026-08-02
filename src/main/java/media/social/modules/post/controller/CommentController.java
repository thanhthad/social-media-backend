package media.social.modules.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.post.dto.request.comment.CreateCommentRequest;
import media.social.modules.post.dto.request.comment.UpdateCommentContent;
import media.social.modules.post.service.CommentService;
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
    @RateLimit(
            name = "COMMENT_CREATE",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> createComment(
            @RequestBody @Valid CreateCommentRequest request
    ) {

        return ResponseData.success(
                commentService.createComment(request),
                "Create comment successfully",
                HttpStatus.CREATED
        );
    }

    // ================= UPDATE COMMENT =================
    @PatchMapping("/{commentId}")
    @Operation(summary = "Update comment content")
    @RateLimit(
            name = "COMMENT_UPDATE",
            limit = 40,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "COMMENT_DELETE",
            limit = 30,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "COMMENT_LIST_POST",
            limit = 300,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "COMMENT_LIST_REPLY",
            limit = 300,
            windowSeconds = 60
    )
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
}