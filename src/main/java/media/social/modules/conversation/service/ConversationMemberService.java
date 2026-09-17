package media.social.modules.conversation.service;

import media.social.modules.conversation.dto.response.ConversationMemberResponse;

import java.util.List;

public interface ConversationMemberService {

    List<ConversationMemberResponse> getConversationMembers(
            Long conversationId
    );

    void addMember(
            Long conversationId,
            Long userId
    );

    void removeMember(
            Long conversationId,
            Long userId
    );

    void updateLastReadMessage(
            Long conversationId,
            Long messageId
    );
}