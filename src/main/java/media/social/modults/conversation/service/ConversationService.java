package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.response.ConversationResponse;
import media.social.modults.conversation.enums.ConversationType;

public interface ConversationService {

    ConversationResponse create(ConversationType type);

    ConversationResponse findById(Long conversationId);

    void delete(Long conversationId);

}