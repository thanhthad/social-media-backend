package media.social.modults.conversation.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.conversation.dto.request.CreateMessageRequest;
import media.social.modults.conversation.dto.response.MessageResponse;
import media.social.modults.conversation.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Message",
        description = "Message APIs"
)
public class MessageController {

    private final MessageService messageService;

    // ================= CREATE MESSAGE =================
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> create(
            @Valid @ModelAttribute CreateMessageRequest request
    ){

        MessageResponse response =
                messageService.create(request);

        return ResponseData.success(
                response,
                "Create message successfully",
                HttpStatus.OK
        );
    }

    // ================= GET MESSAGES =================
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            Pageable pageable
    ){

        Page<MessageResponse> response =
                messageService.getMessages(
                        conversationId,
                        pageable
                );

        return ResponseData.success(
                response,
                "Get messages successfully",
                HttpStatus.OK
        );
    }

    // ================= FIND MESSAGE =================
    @GetMapping("/{messageId}")
    public ResponseEntity<?> findById(
            @PathVariable Long messageId
    ){

        MessageResponse response =
                messageService.findById(
                        messageId
                );

        return ResponseData.success(
                response,
                "Get message successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE MESSAGE =================
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> delete(
            @PathVariable Long messageId
    ){

        messageService.delete(
                messageId
        );

        return ResponseData.success(
                null,
                "Delete message successfully",
                HttpStatus.OK
        );
    }
}