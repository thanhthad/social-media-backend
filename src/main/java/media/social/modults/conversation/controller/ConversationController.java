package media.social.modults.conversation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.dto.request.CreateGroupRequest;
import media.social.modults.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modults.conversation.dto.response.ConversationResponse;
import media.social.modults.conversation.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Conversation",
        description = "Conversation management APIs"
)
public class ConversationController {

    private final ConversationService conversationService;

    // ================= CREATE PRIVATE CONVERSATION =================
    @Operation(
            summary = "Create private conversation",
            description = "Create a private conversation between current user and another user"
    )
    @PostMapping("/private/{targetUserId}")
    @RateLimit(
            name = "CONVERSATION_CREATE_PRIVATE",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> createPrivateConversation(
            @PathVariable Long targetUserId
    ){

        ConversationResponse response =
                conversationService.createPrivateConversation(targetUserId);

        return ResponseData.success(
                response,
                "Create private conversation successfully",
                HttpStatus.CREATED
        );
    }

    // ================= CREATE GROUP CONVERSATION =================
    @Operation(
            summary = "Create group conversation",
            description = "Create a new group conversation"
    )
    @PostMapping(
            value = "/group",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @RateLimit(
            name = "CONVERSATION_CREATE_GROUP",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> createGroupConversation(
            @ModelAttribute CreateGroupRequest request
    ){

        ConversationResponse response =
                conversationService.createGroupConversation(request);

        return ResponseData.success(
                response,
                "Create group conversation successfully",
                HttpStatus.CREATED
        );
    }

    // ================= GET MY CONVERSATIONS =================
    @Operation(
            summary = "Get my conversations",
            description = "Get all conversations of current user"
    )
    @GetMapping
    @RateLimit(
            name = "CONVERSATION_GET_MY_LIST",
            limit = 300,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMyConversations(){

        return ResponseData.success(
                conversationService.getMyConversations(),
                "Get conversations successfully",
                HttpStatus.OK
        );
    }

    // ================= UPDATE GROUP AVATAR =================
    @Operation(
            summary = "Update group avatar",
            description = "Update avatar image of group conversation"
    )
    @PutMapping("/{conversationId}/avatar")
    @RateLimit(
            name = "CONVERSATION_UPDATE_AVATAR",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateGroupAvatar(
            @PathVariable Long conversationId,
            @RequestParam("file") MultipartFile file
    ){

        conversationService.updateGroupAvatar(
                conversationId,
                file
        );

        return ResponseData.success(
                null,
                "Update group avatar successfully",
                HttpStatus.OK
        );
    }

    // ================= UPDATE GROUP NAME =================
    @Operation(
            summary = "Update group name",
            description = "Update name of group conversation"
    )
    @PutMapping("/{conversationId}/name")
    @RateLimit(
            name = "CONVERSATION_UPDATE_NAME",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> updateGroupName(
            @PathVariable Long conversationId,
            @RequestBody UpdateGroupNameRequest request
    ){

        conversationService.updateGroupName(
                conversationId,
                request
        );

        return ResponseData.success(
                null,
                "Update group name successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE CONVERSATION =================
    @Operation(
            summary = "Delete conversation",
            description = "Delete a conversation owned by current user"
    )
    @DeleteMapping("/{conversationId}")
    @RateLimit(
            name = "CONVERSATION_DELETE",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> deleteConversation(
            @PathVariable Long conversationId
    ){

        conversationService.deleteConversation(conversationId);

        return ResponseData.success(
                null,
                "Delete conversation successfully",
                HttpStatus.OK
        );
    }
}