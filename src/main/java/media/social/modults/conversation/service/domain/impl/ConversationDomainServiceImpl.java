package media.social.modults.conversation.service.domain.impl;

import lombok.AllArgsConstructor;
import media.social.modults.conversation.dto.response.ConversationResponse;
import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.enums.ConversationType;
import media.social.modults.conversation.repository.ConversationRepository;
import media.social.modults.conversation.service.domain.ConversationDomainService;
import media.social.modults.post.exception.post.ForbiddenException;
import media.social.modults.user.security.context.UserContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ConversationDomainServiceImpl implements ConversationDomainService {

    private final ConversationRepository conversationRepository;

    @Override
    public Conversation create(ConversationType type) {

        Conversation conversation = Conversation.builder()
                .type(type)
                .build();

        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse findById(Long conversationId) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        return mapToResponse(conversation);
    }

    @Override
    public void delete(Long conversationId) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversationRepository.delete(conversation);
    }

    @Override
    public void checkOwner(Message message) {

        Long currentUserId = UserContextHolder.getUserId();

        if(!message.getSender()
                .getId()
                .equals(currentUserId)){

            throw new ForbiddenException(
                    "You are not allowed to modify this message."
            );
        }
    }

    private ConversationResponse mapToResponse(Conversation conversation) {

        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
