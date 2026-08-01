package media.social.modults.conversation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.service.ConversationMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversation-members")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Conversation Member",
        description = "Conversation member management APIs"
)
public class ConversationMemberController {

    private final ConversationMemberService conversationMemberService;

    // ================= ADD MEMBER =================
    @Operation(
            summary = "Add member to conversation",
            description = "Owner adds a new member into group conversation"
    )
    @PostMapping("/{conversationId}/members/{userId}")
    @RateLimit(
            name = "CONVERSATION_ADD_MEMBER",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> addMember(
            @PathVariable Long conversationId,
            @PathVariable Long userId
    ){

        conversationMemberService.addMember(
                conversationId,
                userId
        );

        return ResponseData.success(
                null,
                "Add member successfully",
                HttpStatus.OK
        );
    }

    // ================= REMOVE MEMBER =================
    @Operation(
            summary = "Remove member from conversation",
            description = "Owner removes a member from group conversation"
    )
    @DeleteMapping("/{conversationId}/members/{userId}")
    @RateLimit(
            name = "CONVERSATION_REMOVE_MEMBER",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> removeMember(
            @PathVariable Long conversationId,
            @PathVariable Long userId
    ){

        conversationMemberService.removeMember(
                conversationId,
                userId
        );

        return ResponseData.success(
                null,
                "Remove member successfully",
                HttpStatus.OK
        );
    }

    // ================= UPDATE LAST READ MESSAGE =================
    @Operation(
            summary = "Update last read message",
            description = "Update user's last read message position in conversation"
    )
    @PutMapping("/{conversationId}/read/{messageId}")
    @RateLimit(
            name = "CONVERSATION_UPDATE_READ",
            limit = 1000,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateLastReadMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ){

        conversationMemberService.updateLastReadMessage(
                conversationId,
                messageId
        );

        return ResponseData.success(
                null,
                "Update read message successfully",
                HttpStatus.OK
        );
    }
}