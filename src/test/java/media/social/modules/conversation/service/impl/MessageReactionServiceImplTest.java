package media.social.modules.conversation.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.conversation.dto.projection.MessageReactionUserProjection;
import media.social.modules.conversation.dto.response.MessageReactionResponse;
import media.social.modules.conversation.dto.response.MessageReactionUserResponse;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.entity.MessageReaction;
import media.social.modules.conversation.exception.MessageNotFoundException;
import media.social.modules.conversation.repository.MessageReactionRepository;
import media.social.modules.conversation.repository.MessageRepository;
import media.social.modules.conversation.websocket.MessageReactionPublisher;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageReactionServiceImplTest {

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private MessageReactionPublisher messageReactionPublisher;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @InjectMocks
    private MessageReactionServiceImpl messageReactionService;

    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;

    // ---------------------------------------------------------------------------
    // react()
    // ---------------------------------------------------------------------------

    @Test
    void react_messageNotFound_throwsMessageNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThrows(MessageNotFoundException.class,
                    () -> messageReactionService.react(MESSAGE_ID, ReactionType.LIKE));

            verify(messageReactionRepository, never()).save(any());
            verify(messageReactionPublisher, never()).send(any(), any());
        }
    }

    @Test
    void react_newReaction_savesAndReturns() {
        Message message = Message.builder().id(MESSAGE_ID).build();
        User user = User.builder().id(USER_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(user);
            when(messageReactionRepository.findByUserIdAndMessageId(USER_ID, MESSAGE_ID)).thenReturn(Optional.empty());

            messageReactionService.react(MESSAGE_ID, ReactionType.LIKE);

            ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
            verify(messageReactionRepository).save(captor.capture());

            MessageReaction saved = captor.getValue();
            assertThat(saved.getMessage()).isEqualTo(message);
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getType()).isEqualTo(ReactionType.LIKE);

            verify(messageReactionPublisher, never()).send(any(), any());
        }
    }

    @Test
    void react_existingReactionDifferentType_updatesSavesAndPublishes() {
        Message message = Message.builder().id(MESSAGE_ID).build();
        User user = User.builder().id(USER_ID).build();
        MessageReaction existingReaction = MessageReaction.builder()
                .id(10L)
                .message(message)
                .user(user)
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(user);
            when(messageReactionRepository.findByUserIdAndMessageId(USER_ID, MESSAGE_ID))
                    .thenReturn(Optional.of(existingReaction));
            when(messageReactionRepository.findAllByMessageId(MESSAGE_ID))
                    .thenReturn(List.of(existingReaction));

            messageReactionService.react(MESSAGE_ID, ReactionType.LOVE);

            assertThat(existingReaction.getType()).isEqualTo(ReactionType.LOVE);
            verify(messageReactionRepository).save(existingReaction);
            verify(messageReactionPublisher).send(eq(USER_ID), any(MessageReactionResponse.class));
        }
    }

    @Test
    void react_existingReactionSameType_doesNotReSave_publishesResponse() {
        Message message = Message.builder().id(MESSAGE_ID).build();
        User user = User.builder().id(USER_ID).build();
        MessageReaction existingReaction = MessageReaction.builder()
                .id(10L)
                .message(message)
                .user(user)
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(user);
            when(messageReactionRepository.findByUserIdAndMessageId(USER_ID, MESSAGE_ID))
                    .thenReturn(Optional.of(existingReaction));
            when(messageReactionRepository.findAllByMessageId(MESSAGE_ID))
                    .thenReturn(List.of(existingReaction));

            messageReactionService.react(MESSAGE_ID, ReactionType.LIKE);

            verify(messageReactionRepository, never()).save(any());
            verify(messageReactionPublisher).send(eq(USER_ID), any(MessageReactionResponse.class));
        }
    }

    // ---------------------------------------------------------------------------
    // removeReaction()
    // ---------------------------------------------------------------------------

    @Test
    void removeReaction_messageNotFound_throwsMessageNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThrows(MessageNotFoundException.class,
                    () -> messageReactionService.removeReaction(MESSAGE_ID));

            verify(messageReactionRepository, never()).delete(any());
        }
    }

    @Test
    void removeReaction_reactionNotFound_throwsRuntimeException() {
        Message message = Message.builder().id(MESSAGE_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(messageReactionRepository.findByUserIdAndMessageId(USER_ID, MESSAGE_ID))
                    .thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> messageReactionService.removeReaction(MESSAGE_ID));

            assertThat(ex.getMessage()).isEqualTo("Reaction not found");
            verify(messageReactionRepository, never()).delete(any());
        }
    }

    @Test
    void removeReaction_success_deletesAndPublishes() {
        Message message = Message.builder().id(MESSAGE_ID).build();
        User user = User.builder().id(USER_ID).build();
        MessageReaction reaction = MessageReaction.builder()
                .id(10L)
                .message(message)
                .user(user)
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(messageReactionRepository.findByUserIdAndMessageId(USER_ID, MESSAGE_ID))
                    .thenReturn(Optional.of(reaction));
            when(messageReactionRepository.findAllByMessageId(MESSAGE_ID))
                    .thenReturn(Collections.emptyList());

            messageReactionService.removeReaction(MESSAGE_ID);

            verify(messageReactionRepository).delete(reaction);
            verify(messageReactionPublisher).send(eq(USER_ID), any(MessageReactionResponse.class));
        }
    }

    // ---------------------------------------------------------------------------
    // getUsersReacted()
    // ---------------------------------------------------------------------------

    @Test
    void getUsersReacted_messageNotFound_throwsMessageNotFoundException() {
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

        assertThrows(MessageNotFoundException.class,
                () -> messageReactionService.getUsersReacted(MESSAGE_ID));

        verify(messageReactionRepository, never()).findUsersReacted(any());
    }

    @Test
    void getUsersReacted_success() {
        Message message = Message.builder().id(MESSAGE_ID).build();
        MessageReactionUserProjection projection = mock(MessageReactionUserProjection.class);
        when(projection.getUserId()).thenReturn(1L);
        when(projection.getUserName()).thenReturn("alice");
        when(projection.getAvatarUrl()).thenReturn("https://example.com/avatar.png");
        when(projection.getReactionType()).thenReturn(ReactionType.LIKE);
        OffsetDateTime now = OffsetDateTime.now();
        when(projection.getCreatedAt()).thenReturn(now);

        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
        when(messageReactionRepository.findUsersReacted(MESSAGE_ID)).thenReturn(List.of(projection));

        List<MessageReactionUserResponse> responses = messageReactionService.getUsersReacted(MESSAGE_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo(1L);
        assertThat(responses.get(0).getUserName()).isEqualTo("alice");
        assertThat(responses.get(0).getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(responses.get(0).getReactionType()).isEqualTo(ReactionType.LIKE);
        assertThat(responses.get(0).getCreatedAt()).isEqualTo(now);
    }
}
