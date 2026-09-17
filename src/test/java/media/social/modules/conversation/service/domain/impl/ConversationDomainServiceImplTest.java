package media.social.modules.conversation.service.domain.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.conversation.dto.response.ConversationResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.post.exception.post.ForbiddenException;
import media.social.modules.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationDomainServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationDomainServiceImpl conversationDomainService;

    @Test
    void create_success() {
        Conversation conversation = Conversation.builder()
                .id(1L)
                .type(ConversationType.PRIVATE)
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation result = conversationDomainService.create(ConversationType.PRIVATE);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(ConversationType.PRIVATE);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void findById_found_success() {
        OffsetDateTime now = OffsetDateTime.now();
        Conversation conversation = Conversation.builder()
                .id(10L)
                .type(ConversationType.GROUP)
                .createdAt(now)
                .build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        ConversationResponse response = conversationDomainService.findById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getType()).isEqualTo(ConversationType.GROUP);
        assertThat(response.getCreatedAt()).isEqualTo(now);
        verify(conversationRepository).findById(10L);
    }

    @Test
    void findById_notFound_throwsRuntimeException() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> conversationDomainService.findById(99L));

        assertThat(ex.getMessage()).isEqualTo("Conversation not found");
        verify(conversationRepository).findById(99L);
    }

    @Test
    void delete_found_success() {
        Conversation conversation = Conversation.builder()
                .id(10L)
                .build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        conversationDomainService.delete(10L);

        verify(conversationRepository).findById(10L);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void delete_notFound_throwsRuntimeException() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> conversationDomainService.delete(99L));

        assertThat(ex.getMessage()).isEqualTo("Conversation not found");
        verify(conversationRepository).findById(99L);
        verify(conversationRepository, never()).delete(any());
    }

    @Test
    void checkOwner_success() {
        Long userId = 1L;
        User sender = User.builder().id(userId).build();
        Message message = Message.builder().id(100L).sender(sender).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            conversationDomainService.checkOwner(message);
        }
    }

    @Test
    void checkOwner_notOwner_throwsForbiddenException() {
        Long userId = 1L;
        User sender = User.builder().id(2L).build();
        Message message = Message.builder().id(100L).sender(sender).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> conversationDomainService.checkOwner(message));

            assertThat(ex.getMessage()).isEqualTo("You are not allowed to modify this message.");
        }
    }
}
