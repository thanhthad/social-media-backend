package media.social.modules.conversation.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.conversation.dto.request.CreateMessageRequest;
import media.social.modules.conversation.dto.response.MessageResponse;
import media.social.modules.conversation.service.MessageService;
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
    @RateLimit(
            name = "MESSAGE_CREATE",
            limit = 60,
            windowSeconds = 60
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
    @RateLimit(
            name = "MESSAGE_GET_LIST",
            limit = 300,
            windowSeconds = 60
    )
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

    // ================= DELETE MESSAGE =================
    @DeleteMapping("/{messageId}")
    @RateLimit(
            name = "MESSAGE_DELETE",
            limit = 30,
            windowSeconds = 60
    )
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