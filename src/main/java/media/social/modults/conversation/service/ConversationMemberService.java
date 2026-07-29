package media.social.modults.conversation.service;

import media.social.modults.conversation.dto.request.CreateGroupRequest;
import media.social.modults.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modults.conversation.dto.response.ConversationListResponse;
import media.social.modults.conversation.dto.response.ConversationMemberResponse;
import media.social.modults.conversation.dto.response.ConversationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConversationMemberService {

    void addMember(
            Long conversationId,
            Long userId
    );

    void removeMember(
            Long conversationId,
            Long userId
    );

    boolean isMember(
            Long conversationId,
            Long userId
    );

    void updateLastReadMessage(
            Long conversationId,
            Long messageId
    );
}