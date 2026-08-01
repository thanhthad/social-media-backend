package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.reaction.ReactionRequest;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.post.service.ReactionService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    // ================= ADD / UPDATE REACTION =================
    @PostMapping("/{postId}/reaction")
    @Operation(summary = "React or update reaction for post")
    public ResponseEntity<?> react(
            @PathVariable Long postId,
            @RequestBody @Valid ReactionRequest request
    ) {

        reactionService.react(
                postId,
                request.getType()
        );

        return ResponseData.success(
                null,
                "Reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= REMOVE REACTION =================
    @DeleteMapping("/{postId}/reaction")
    @Operation(summary = "Remove reaction from post")
    public ResponseEntity<?> removeReaction(
            @PathVariable Long postId
    ) {

        reactionService.removeReaction(postId);

        return ResponseData.success(
                null,
                "Remove reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= COUNT REACTION =================
    @GetMapping("/{postId}/reactions/count")
    @Operation(summary = "Count reactions of post")
    public ResponseEntity<?> countReaction(
            @PathVariable Long postId
    ) {

        return ResponseData.success(
                reactionService.countReaction(postId),
                "Count reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= GET USERS REACTED =================
    @GetMapping("/{postId}/reactions")
    @Operation(summary = "Get users reacted to post")
    public ResponseEntity<?> getUsersReacted(
            @PathVariable Long postId,
            @RequestParam(required = false) ReactionType type,
            Pageable pageable
    ) {

        return ResponseData.success(
                reactionService.getUsersReacted(
                        postId,
                        type,
                        pageable
                ),
                "Get users reacted successfully",
                HttpStatus.OK
        );
    }
}