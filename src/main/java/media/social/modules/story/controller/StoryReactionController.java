package media.social.modules.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modules.post.dto.request.reaction.ReactionRequest;
import media.social.modules.story.service.StoryReactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryReactionController {

    private final StoryReactionService storyReactionService;

    // ================= ADD / UPDATE REACTION =================
    @PostMapping("/{storyId}/reaction")
    @Operation(summary = "React or update reaction for story")
    public ResponseEntity<?> react(
            @PathVariable Long storyId,
            @RequestBody @Valid ReactionRequest request
    ) {

        storyReactionService.react(
                storyId,
                request.getType()
        );

        return ResponseData.success(
                null,
                "Reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= REMOVE REACTION =================
    @DeleteMapping("/{storyId}/reaction")
    @Operation(summary = "Remove reaction from story")
    public ResponseEntity<?> removeReaction(
            @PathVariable Long storyId
    ) {

        storyReactionService.removeReaction(storyId);

        return ResponseData.success(
                null,
                "Remove reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= COUNT REACTION =================
    @GetMapping("/{storyId}/reactions/count")
    @Operation(summary = "Count reactions of story")
    public ResponseEntity<?> countReaction(
            @PathVariable Long storyId
    ) {

        return ResponseData.success(
                storyReactionService.countReaction(storyId),
                "Count reaction successfully",
                HttpStatus.OK
        );
    }
}