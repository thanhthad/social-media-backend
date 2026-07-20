package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.response.ConversationMemberResponse;

import java.util.List;

public interface ConversationMemberService {

    void addMember(Long conversationId, Long userId);

    void removeMember(Long conversationId, Long userId);

    boolean isMember(Long conversationId, Long userId);

    List<ConversationMemberResponse> getMembers(Long conversationId);

    void updateLastReadMessage(Long conversationId, Long userId, Long messageId);
}