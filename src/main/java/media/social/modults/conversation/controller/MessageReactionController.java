package media.social.modults.conversation.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
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
    @RateLimit(
            name = "MESSAGE_REACTION_CREATE",
            limit = 120,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "MESSAGE_REACTION_DELETE",
            limit = 60,
            windowSeconds = 60
    )
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
    @RateLimit(
            name = "MESSAGE_REACTION_GET_USERS",
            limit = 300,
            windowSeconds = 60
    )
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