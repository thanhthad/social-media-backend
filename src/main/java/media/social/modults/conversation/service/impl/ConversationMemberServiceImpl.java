package media.social.modults.conversation.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.entity.ConversationMember;
import media.social.modults.conversation.entity.ConversationMemberId;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.enums.ConversationType;
import media.social.modults.conversation.exception.*;
import media.social.modults.conversation.repository.ConversationMemberRepository;
import media.social.modults.conversation.repository.ConversationRepository;
import media.social.modults.conversation.repository.MessageRepository;
import media.social.modults.conversation.service.ConversationMemberService;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
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
    @Transactional(readOnly = true)
    public boolean isMember(Long conversationId, Long userId) {

        return conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
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