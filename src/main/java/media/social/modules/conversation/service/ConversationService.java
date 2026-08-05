package media.social.modules.conversation.service;

import media.social.modules.conversation.dto.request.CreateGroupRequest;
import media.social.modules.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modules.conversation.dto.response.ConversationListResponse;
import media.social.modules.conversation.dto.response.ConversationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConversationService {

    ConversationResponse createPrivateConversation(Long targetUserId);

    ConversationResponse createGroupConversation(
            CreateGroupRequest request
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

    ConversationResponse getConversationDetail(
            Long conversationId
    );

    List<ConversationListResponse> getMyConversations();
}