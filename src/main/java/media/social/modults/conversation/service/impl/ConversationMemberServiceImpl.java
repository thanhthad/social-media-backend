package media.social.modults.conversation.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.conversation.dto.response.ConversationMemberResponse;
import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.entity.ConversationMember;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.exception.ConversationNotFoundException;
import media.social.modults.conversation.exception.MemberNotFoundException;
import media.social.modults.conversation.exception.MessageNotFoundException;
import media.social.modults.conversation.exception.UserInMemberAlreadyExists;
import media.social.modults.conversation.repository.ConversationMemberRepository;
import media.social.modults.conversation.repository.ConversationRepository;
import media.social.modults.conversation.repository.MessageRepository;
import media.social.modults.conversation.service.ConversationMemberService;
import media.social.modults.user.entity.User;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));

        User user = userServiceDomain.getByUserId(userId);

        if (conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new UserInMemberAlreadyExists("User already exists");
        }

        ConversationMember member = ConversationMember.builder()
                .conversation(conversation)
                .user(user)
                .build();

        conversationMemberRepository.save(member);
    }


    @Override
    @Transactional
    public void removeMember(Long conversationId, Long userId) {

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new MemberNotFoundException("Member not found");
        }

        conversationMemberRepository.deleteByConversationIdAndUserId(conversationId, userId);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long conversationId, Long userId) {

        return conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ConversationMemberResponse> getMembers(Long conversationId) {

        return conversationMemberRepository.findMembersByConversationId(conversationId);
    }


    @Override
    @Transactional
    public void updateLastReadMessage(Long conversationId, Long userId, Long messageId) {

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        member.setLastReadMessage(message);
    }


    private ConversationMemberResponse mapToResponse(ConversationMember member) {

        User user = member.getUser();

        return ConversationMemberResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .build();
    }
}