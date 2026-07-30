package media.social.modults.conversation.service.domain;

import media.social.modults.conversation.dto.response.ConversationResponse;
import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.enums.ConversationType;

public interface ConversationDomainService {

    Conversation create(ConversationType type);

    ConversationResponse findById(Long conversationId);

    void delete(Long conversationId);

    void checkOwner(Message message);



}
