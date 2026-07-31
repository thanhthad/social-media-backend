package media.social.modults.conversation.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.service.MessageReactionService;
import media.social.modults.post.enums.ReactionType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages/reactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Message Reaction",
        description = "Message Reaction APIs"
)
public class MessageReactionController {

    private final MessageReactionService messageReactionService;


    // ================= REACT MESSAGE =================
    @PostMapping("/{messageId}")
    public ResponseEntity<?> react(
            @PathVariable Long messageId,
            @RequestParam ReactionType type
    ){

        messageReactionService.react(
                messageId,
                type
        );

        return ResponseData.success(
                null,
                "React message successfully",
                HttpStatus.OK
        );
    }


    // ================= REMOVE REACTION =================
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> removeReaction(
            @PathVariable Long messageId
    ){

        messageReactionService.removeReaction(
                messageId
        );

        return ResponseData.success(
                null,
                "Remove reaction successfully",
                HttpStatus.OK
        );
    }

    // ================= GET USERS REACTED =================
    @GetMapping("/{messageId}/users")
    public ResponseEntity<?> getUsersReacted(
            @PathVariable Long messageId
    ){

        return ResponseData.success(
                messageReactionService.getUsersReacted(messageId),
                "Get users reacted successfully",
                HttpStatus.OK
        );
    }


}