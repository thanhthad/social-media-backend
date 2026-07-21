package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.request.CreateGroupRequest;
import media.social.modults.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modults.conversation.dto.response.ConversationListResponse;
import media.social.modults.conversation.dto.response.ConversationMemberResponse;
import media.social.modults.conversation.dto.response.ConversationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConversationMemberService {

    ConversationResponse createPrivateConversation(Long targetUserId);

    ConversationResponse getConversationDetail(
            Long conversationId
    );

    void updateGroupAvatar(
            Long conversationId,
            MultipartFile file
    );

    void updateGroupName(
            Long conversationId,
            UpdateGroupNameRequest request
    );

    void deleteConversation(Long conversationId);

    ConversationResponse createGroupConversation(CreateGroupRequest request);

    List<ConversationListResponse> getMyConversations();

    void addMember(Long conversationId, Long userId);

    void removeMember(Long conversationId, Long userId);

    boolean isMember(Long conversationId, Long userId);

    List<ConversationMemberResponse> getMembers(Long conversationId);

    void updateLastReadMessage(Long conversationId, Long messageId);
}