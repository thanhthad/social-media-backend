package media.social.modults.conversation.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.dto.response.MessageReactionCountResponse;
import media.social.modults.conversation.dto.response.MessageReactionResponse;
import media.social.modults.conversation.dto.response.MessageReactionUserResponse;
import media.social.modults.conversation.service.MessageReactionService;
import media.social.modults.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    // ================= GET MY REACTION =================
    @GetMapping("/{messageId}/me")
    public ResponseEntity<?> getMyReaction(
            @PathVariable Long messageId
    ){

        MessageReactionResponse response =
                messageReactionService.getMyReaction(
                        messageId
                );

        return ResponseData.success(
                response,
                "Get my reaction successfully",
                HttpStatus.OK
        );
    }


    // ================= COUNT REACTION =================
    @GetMapping("/{messageId}/count")
    public ResponseEntity<?> countReaction(
            @PathVariable Long messageId
    ){

        MessageReactionCountResponse response =
                messageReactionService.countReaction(
                        messageId
                );


        return ResponseData.success(
                response,
                "Count reaction successfully",
                HttpStatus.OK
        );
    }



    // ================= USERS REACTED =================
    @GetMapping("/{messageId}/users")
    public ResponseEntity<?> getUsersReacted(
            @PathVariable Long messageId,
            @RequestParam(required = false) ReactionType type,
            Pageable pageable
    ){

        Page<MessageReactionUserResponse> response =
                messageReactionService.getUsersReacted(
                        messageId,
                        type,
                        pageable
                );


        return ResponseData.success(
                response,
                "Get users reacted successfully",
                HttpStatus.OK
        );
    }
}