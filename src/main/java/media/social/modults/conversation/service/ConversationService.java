package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.response.ConversationResponse;
import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.enums.ConversationType;

public interface ConversationService {

    Conversation create(ConversationType type);

    ConversationResponse findById(Long conversationId);

    void delete(Long conversationId);


}