package media.social.modules.conversation.service.domain;

import media.social.modules.conversation.dto.response.ConversationResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.enums.ConversationType;

public interface ConversationDomainService {

    Conversation create(ConversationType type);

    ConversationResponse findById(Long conversationId);

    void delete(Long conversationId);

    void checkOwner(Message message);



}
