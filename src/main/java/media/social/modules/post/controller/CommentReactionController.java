package media.social.modules.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.post.dto.request.reaction.ReactionRequest;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.service.CommentReactionService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentReactionController {

    private final CommentReactionService commentReactionService;

    // ================= ADD / UPDATE REACTION =================
    @PostMapping("/{commentId}/reaction")
    @Operation(summary = "React or update reaction for comment")
    @RateLimit(
            name = "COMMENT_REACTION_CREATE",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> react(
            @PathVariable Long commentId,
            @RequestBody @Valid ReactionRequest request
    ) {

        commentReactionService.react(
                commentId,
                request.getType()
        );

        return ResponseData.success(
                null,
                "Reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= REMOVE REACTION =================
    @DeleteMapping("/{commentId}/reaction")
    @Operation(summary = "Remove reaction from comment")
    @RateLimit(
            name = "COMMENT_REACTION_DELETE",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> removeReaction(
            @PathVariable Long commentId
    ) {

        commentReactionService.removeReaction(commentId);

        return ResponseData.success(
                null,
                "Remove reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= GET MY REACTION =================
    @GetMapping("/{commentId}/reaction/me")
    @Operation(summary = "Get current user's reaction")
    @RateLimit(
            name = "COMMENT_MY_REACTION",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMyReaction(
            @PathVariable Long commentId
    ) {

        return ResponseData.success(
                commentReactionService.getMyReaction(commentId),
                "Get my reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= COUNT REACTION =================
    @GetMapping("/{commentId}/reactions/count")
    @Operation(summary = "Count reactions of comment")
    @RateLimit(
            name = "COMMENT_REACTION_COUNT",
            limit = 500,
            windowSeconds = 60
    )
    public ResponseEntity<?> countReaction(
            @PathVariable Long commentId
    ) {

        return ResponseData.success(
                commentReactionService.countReaction(commentId),
                "Count reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= GET USERS REACTED =================
    @GetMapping("/{commentId}/reactions")
    @Operation(summary = "Get users reacted to comment")
    @RateLimit(
            name = "COMMENT_REACTION_USERS",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getUsersReacted(
            @PathVariable Long commentId,
            @RequestParam(required = false) ReactionType type,
            Pageable pageable
    ) {

        return ResponseData.success(
                commentReactionService.getUsersReacted(
                        commentId,
                        type,
                        pageable
                ),
                "Get users reacted successfully",
                HttpStatus.OK
        );
    }
}