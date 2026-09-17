package media.social.modules.conversation.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.conversation.dto.projection.ConversationMemberProjection;
import media.social.modules.conversation.dto.response.ConversationMemberResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.exception.ConversationNotFoundException;
import media.social.modules.conversation.exception.MemberNotFoundException;
import media.social.modules.conversation.exception.MessageNotFoundException;
import media.social.modules.conversation.exception.UserInMemberAlreadyExists;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.conversation.repository.MessageRepository;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationMemberServiceImplTest {

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @InjectMocks
    private ConversationMemberServiceImpl conversationMemberService;

    private static final Long CONVERSATION_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long USER_ID = 2L;

    // ---------------------------------------------------------------------------
    // getConversationMembers
    // ---------------------------------------------------------------------------

    @Test
    void getConversationMembers_success() {
        ConversationMemberProjection proj1 = mock(ConversationMemberProjection.class);
        when(proj1.getUserId()).thenReturn(1L);
        when(proj1.getUsername()).thenReturn("alice");
        when(proj1.getAvatarUrl()).thenReturn("https://example.com/alice.png");

        ConversationMemberProjection proj2 = mock(ConversationMemberProjection.class);
        when(proj2.getUserId()).thenReturn(2L);
        when(proj2.getUsername()).thenReturn("bob");
        when(proj2.getAvatarUrl()).thenReturn("https://example.com/bob.png");

        when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                .thenReturn(List.of(proj1, proj2));

        List<ConversationMemberResponse> responses = conversationMemberService.getConversationMembers(CONVERSATION_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getUserId()).isEqualTo(1L);
        assertThat(responses.get(0).getUsername()).isEqualTo("alice");
        assertThat(responses.get(0).getAvatarUrl()).isEqualTo("https://example.com/alice.png");
        assertThat(responses.get(1).getUserId()).isEqualTo(2L);
        assertThat(responses.get(1).getUsername()).isEqualTo("bob");
        assertThat(responses.get(1).getAvatarUrl()).isEqualTo("https://example.com/bob.png");

        verify(conversationMemberRepository).findMembersProjectionByConversationId(CONVERSATION_ID);
    }

    // ---------------------------------------------------------------------------
    // addMember
    // ---------------------------------------------------------------------------

    @Test
    void addMember_success() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        User userToAdd = User.builder().id(USER_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);

            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(userToAdd);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(false);

            conversationMemberService.addMember(CONVERSATION_ID, USER_ID);

            ArgumentCaptor<ConversationMember> captor = ArgumentCaptor.forClass(ConversationMember.class);
            verify(conversationMemberRepository).save(captor.capture());

            ConversationMember saved = captor.getValue();
            assertThat(saved.getConversation()).isEqualTo(conversation);
            assertThat(saved.getUser()).isEqualTo(userToAdd);
            assertThat(saved.getId().getConversationId()).isEqualTo(CONVERSATION_ID);
            assertThat(saved.getId().getUserId()).isEqualTo(USER_ID);
        }
    }

    @Test
    void addMember_conversationNotFound_throwsConversationNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> conversationMemberService.addMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).save(any());
        }
    }

    @Test
    void addMember_notOwner_throwsAccessDeniedException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(999L); // not owner

            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(AccessDeniedException.class,
                    () -> conversationMemberService.addMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).save(any());
        }
    }

    @Test
    void addMember_privateConversation_throwsIllegalArgumentException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.PRIVATE)
                .owner(owner)
                .build();

        User userToAdd = User.builder().id(USER_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);

            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(userToAdd);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> conversationMemberService.addMember(CONVERSATION_ID, USER_ID));

            assertThat(ex.getMessage()).isEqualTo("This is a PRIVATE conversation");
            verify(conversationMemberRepository, never()).save(any());
        }
    }

    @Test
    void addMember_datingConversation_throwsIllegalArgumentException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.DATING)
                .owner(owner)
                .build();

        User userToAdd = User.builder().id(USER_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);

            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(userToAdd);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> conversationMemberService.addMember(CONVERSATION_ID, USER_ID));

            assertThat(ex.getMessage()).isEqualTo("This is a PRIVATE conversation");
            verify(conversationMemberRepository, never()).save(any());
        }
    }

    @Test
    void addMember_alreadyExists_throwsUserInMemberAlreadyExists() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        User userToAdd = User.builder().id(USER_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);

            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(userToAdd);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);

            assertThrows(UserInMemberAlreadyExists.class,
                    () -> conversationMemberService.addMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------------
    // removeMember
    // ---------------------------------------------------------------------------

    @Test
    void removeMember_success() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);

            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);

            conversationMemberService.removeMember(CONVERSATION_ID, USER_ID);

            verify(conversationMemberRepository).deleteByConversationIdAndUserId(CONVERSATION_ID, USER_ID);
        }
    }

    @Test
    void removeMember_conversationNotFound_throwsConversationNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> conversationMemberService.removeMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).deleteByConversationIdAndUserId(any(), any());
        }
    }

    @Test
    void removeMember_privateConversation_throwsIllegalArgumentException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.PRIVATE)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(IllegalArgumentException.class,
                    () -> conversationMemberService.removeMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).deleteByConversationIdAndUserId(any(), any());
        }
    }

    @Test
    void removeMember_datingConversation_throwsIllegalArgumentException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.DATING)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(IllegalArgumentException.class,
                    () -> conversationMemberService.removeMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).deleteByConversationIdAndUserId(any(), any());
        }
    }

    @Test
    void removeMember_memberNotFound_throwsMemberNotFoundException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(false);

            assertThrows(MemberNotFoundException.class,
                    () -> conversationMemberService.removeMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).deleteByConversationIdAndUserId(any(), any());
        }
    }

    @Test
    void removeMember_ownerRemovesHimself_throwsIllegalArgumentException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(OWNER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, OWNER_ID)).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> conversationMemberService.removeMember(CONVERSATION_ID, OWNER_ID));

            assertThat(ex.getMessage()).isEqualTo("Owner cannot remove himself");
            verify(conversationMemberRepository, never()).deleteByConversationIdAndUserId(any(), any());
        }
    }

    @Test
    void removeMember_notOwner_throwsAccessDeniedException() {
        User owner = User.builder().id(OWNER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(999L); // not owner
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);

            assertThrows(AccessDeniedException.class,
                    () -> conversationMemberService.removeMember(CONVERSATION_ID, USER_ID));

            verify(conversationMemberRepository, never()).deleteByConversationIdAndUserId(any(), any());
        }
    }

    // ---------------------------------------------------------------------------
    // updateLastReadMessage
    // ---------------------------------------------------------------------------

    @Test
    void updateLastReadMessage_success() {
        Long messageId = 50L;
        ConversationMember member = new ConversationMember();
        Message message = Message.builder().id(messageId).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(conversationMemberRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

            conversationMemberService.updateLastReadMessage(CONVERSATION_ID, messageId);

            assertThat(member.getLastReadMessage()).isEqualTo(message);
        }
    }

    @Test
    void updateLastReadMessage_memberNotFound_throwsMemberNotFoundException() {
        Long messageId = 50L;

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(conversationMemberRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(MemberNotFoundException.class,
                    () -> conversationMemberService.updateLastReadMessage(CONVERSATION_ID, messageId));

            verify(messageRepository, never()).findById(any());
        }
    }

    @Test
    void updateLastReadMessage_messageNotFound_throwsMessageNotFoundException() {
        Long messageId = 50L;
        ConversationMember member = new ConversationMember();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(conversationMemberRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            assertThrows(MessageNotFoundException.class,
                    () -> conversationMemberService.updateLastReadMessage(CONVERSATION_ID, messageId));
        }
    }
}
