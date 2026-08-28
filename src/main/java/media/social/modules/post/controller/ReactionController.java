package media.social.modules.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modules.post.dto.request.reaction.ReactionRequest;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.service.ReactionService;
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

        return ResponseData.success(
                reactionService.react(
                        postId,
                        request.getType()
                ),
                "Reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= REEL REACTION =================

    @PostMapping("/{postId}/reel/reaction")
    @Operation(summary = "Love or unlove a reel")
    public ResponseEntity<?> loveReel(
            @PathVariable Long postId
    ) {
        return ResponseData.success(
                reactionService.loveReel(postId),
                "Reel reaction successfully",
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