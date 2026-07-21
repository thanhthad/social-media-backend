package media.social.modults.conversation.controller;

import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.dto.request.CreateGroupRequest;
import media.social.modults.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modults.conversation.dto.response.ConversationMemberResponse;
import media.social.modults.conversation.dto.response.ConversationResponse;
import media.social.modults.conversation.service.ConversationMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationMemberController {

    private final ConversationMemberService conversationMemberService;

    @PostMapping("/private/{targetUserId}")
    public ResponseEntity<?> createPrivateConversation(
            @PathVariable Long targetUserId
    ){

        ConversationResponse response =
                conversationMemberService.createPrivateConversation(
                        targetUserId
                );

        return ResponseData.success(
                response,
                "Create private conversation successfully",
                HttpStatus.CREATED
        );
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroupConversation(
            @RequestBody CreateGroupRequest request
    ){

        ConversationResponse response =
                conversationMemberService.createGroupConversation(
                        request
                );

        return ResponseData.success(
                response,
                "Create group conversation successfully",
                HttpStatus.CREATED
        );
    }
    @GetMapping
    public ResponseEntity<?> getMyConversations(){

        return ResponseData.success(
                conversationMemberService.getMyConversations(),
                "Get conversations successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/{conversationId}/avatar")
    public ResponseEntity<?> updateGroupAvatar(
            @PathVariable Long conversationId,
            @RequestParam("file") MultipartFile file
    ){

        conversationMemberService.updateGroupAvatar(
                conversationId,
                file
        );

        return ResponseData.success(
                null,
                "Update group avatar successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/{conversationId}/name")
    public ResponseEntity<?> updateGroupName(
            @PathVariable Long conversationId,
            @RequestBody UpdateGroupNameRequest request
    ){
        conversationMemberService.updateGroupName(
                conversationId,
                request
        );

        return ResponseData.success(
                null,
                "Update group name successfully",
                HttpStatus.OK
        );
    }
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> deleteConversation(
            @PathVariable Long conversationId
    ){

        conversationMemberService.deleteConversation(
                conversationId
        );

        return ResponseData.success(
                null,
                "Delete conversation successfully",
                HttpStatus.OK
        );
    }
    @PostMapping("/{conversationId}/members/{userId}")
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

    @DeleteMapping("/{conversationId}/members/{userId}")
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

    @GetMapping("/{conversationId}/members")
    public ResponseEntity<?> getMembers(
            @PathVariable Long conversationId
    ){

        List<ConversationMemberResponse> response =
                conversationMemberService.getMembers(
                        conversationId
                );
        return ResponseData.success(
                response,
                "Get members successfully",
                HttpStatus.OK
        );
    }
    @PutMapping("/{conversationId}/read/{messageId}")
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