package media.social.modults.conversation.controller;

import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.dto.response.ConversationMemberResponse;
import media.social.modults.conversation.service.ConversationMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationMemberController {

    private final ConversationMemberService conversationMemberService;

    @PostMapping("/{conversationId}/members/{userId}")
    public ResponseEntity<?> addMember(
            @PathVariable Long conversationId,
            @PathVariable Long userId
    ) {

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
    ) {
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
    ) {

        List<ConversationMemberResponse> response =
                conversationMemberService.getMembers(conversationId);

        return ResponseData.success(
                response,
                "Get members successfully",
                HttpStatus.OK
        );
    }

}