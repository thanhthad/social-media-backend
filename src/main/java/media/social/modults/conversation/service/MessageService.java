package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.request.CreateMessageRequest;
import media.social.modults.conversation.dto.request.UpdateMessageRequest;
import media.social.modults.conversation.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    MessageResponse create(
            CreateMessageRequest request
    );

    Page<MessageResponse> getMessages(
            Long conversationId,
            Pageable pageable
    );

    MessageResponse findById(
            Long messageId
    );

    MessageResponse update(
            Long messageId,
            UpdateMessageRequest request
    );

    void delete(
            Long messageId
    );

}