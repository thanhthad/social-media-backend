package media.social.modules.conversation.service;

public interface ConversationMemberService {

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