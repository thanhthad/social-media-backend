package media.social.modules.conversation.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.exception.*;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.conversation.repository.MessageRepository;
import media.social.modules.conversation.service.ConversationMemberService;
import media.social.modules.user.entity.User;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ConversationMemberServiceImpl implements ConversationMemberService {

    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserServiceDomain userServiceDomain;

    @Override
    @Transactional
    public void addMember(Long conversationId, Long userId) {

        Conversation conversation = conversationRepository.findByIdWithOwner(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));

        Long currentUserId =
                UserContextHolder.getUserId();

        if(!conversation.getOwner()
                .getId()
                .equals(currentUserId)){

            throw new AccessDeniedException(
                    "Only owner can add member"
            );
        }
        User user = userServiceDomain.getByUserId(userId);

        if(conversation.getType().equals(ConversationType.PRIVATE)){
            throw new IllegalArgumentException("This is a PRIVATE conversation");
        }

        if(conversation.getType().equals(ConversationType.DATING)){
            throw new IllegalArgumentException("This is a PRIVATE conversation");
        }

        if (conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new UserInMemberAlreadyExists("User already exists");
        }

        ConversationMember member =
                ConversationMember.builder()
                        .id(
                                new ConversationMemberId(
                                        conversationId,
                                        userId
                                )
                        )
                        .conversation(conversation)
                        .user(user)
                        .build();

        conversationMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdWithOwner(conversationId).orElseThrow(
                () -> new ConversationNotFoundException("Conversation not found")
        );
        Long currentUserId =
                UserContextHolder.getUserId();

        if(conversation.getType().equals(ConversationType.PRIVATE)){
            throw new IllegalArgumentException("This is a PRIVATE conversation");
        }

        if(conversation.getType().equals(ConversationType.DATING)){
            throw new IllegalArgumentException("This is a PRIVATE conversation");
        }

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new MemberNotFoundException("Member not found");
        }

        if(userId.equals(conversation.getOwner().getId())){
            throw new IllegalArgumentException(
                    "Owner cannot remove himself"
            );
        }

        if(!conversation.getOwner()
                .getId()
                .equals(currentUserId)){

            throw new AccessDeniedException(
                    "Only owner can remove member"
            );
        }

        conversationMemberRepository.deleteByConversationIdAndUserId(conversationId, userId);
    }

    @Override
    @Transactional
    public void updateLastReadMessage(
            Long conversationId,
            Long messageId
    ){
        Long userId = UserContextHolder.getUserId();
        ConversationMember member =
                conversationMemberRepository
                        .findByConversationIdAndUserId(
                                conversationId,
                                userId
                        )
                        .orElseThrow(
                                () -> new MemberNotFoundException(
                                        "Member not found"
                                )
                        );
        Message message =
                messageRepository.findById(messageId)
                        .orElseThrow(
                                () -> new MessageNotFoundException(
                                        "Message not found"
                                )
                        );
        member.setLastReadMessage(message);
    }
}