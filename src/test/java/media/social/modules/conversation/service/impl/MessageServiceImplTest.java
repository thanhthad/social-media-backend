package media.social.modules.conversation.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.conversation.dto.projection.MessageProjection;
import media.social.modules.conversation.dto.request.CreateMessageRequest;
import media.social.modules.conversation.dto.response.MessageResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.entity.MessageMedia;
import media.social.modules.conversation.entity.MessageReaction;
import media.social.modules.conversation.exception.ConversationNotFoundException;
import media.social.modules.conversation.exception.InvalidMediaException;
import media.social.modules.conversation.exception.MessageNotFoundException;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.conversation.repository.MessageMediaRepository;
import media.social.modules.conversation.repository.MessageReactionRepository;
import media.social.modules.conversation.repository.MessageRepository;
import media.social.modules.conversation.service.domain.ConversationDomainService;
import media.social.modules.conversation.websocket.MessagePublisher;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private ConversationDomainService conversationDomainService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private MessageMediaRepository messageMediaRepository;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private static final Long USER_ID = 1L;
    private static final Long CONVERSATION_ID = 10L;
    private static final Long MESSAGE_ID = 100L;

    // ---------------------------------------------------------------------------
    // create() validation tests
    // ---------------------------------------------------------------------------

    @Test
    void create_emptyMessage_throwsIllegalArgumentException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("");
        request.setFiles(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> messageService.create(request));

        assertThat(ex.getMessage()).isEqualTo("Message cannot be empty");
    }

    @Test
    void create_messageTooLong_throwsIllegalArgumentException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("a".repeat(5001));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> messageService.create(request));

        assertThat(ex.getMessage()).isEqualTo("Message too long");
    }

    @Test
    void create_tooManyFiles_throwsIllegalArgumentException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("hello");
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(mock(MultipartFile.class));
        }
        request.setFiles(files);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> messageService.create(request));

        assertThat(ex.getMessage()).isEqualTo("Maximum 10 files allowed");
    }

    @Test
    void create_conversationNotFound_throwsConversationNotFoundException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Hello");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> messageService.create(request));
        }
    }

    @Test
    void create_notMember_throwsAccessDeniedException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Hello");

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(false);

            assertThrows(AccessDeniedException.class,
                    () -> messageService.create(request));
        }
    }

    @Test
    void create_replyMessageNotFound_throwsMessageNotFoundException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Hello reply");
        request.setReplyToMessageId(999L);

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        User sender = User.builder().id(USER_ID).username("alice").build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(true);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(sender);
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(MessageNotFoundException.class,
                    () -> messageService.create(request));
        }
    }

    @Test
    void create_replyMessageDifferentConversation_throwsIllegalArgumentException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Hello reply");
        request.setReplyToMessageId(999L);

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        Conversation otherConversation = Conversation.builder().id(9999L).build();
        Message replyMessage = Message.builder().id(999L).conversation(otherConversation).build();
        User sender = User.builder().id(USER_ID).username("alice").build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(true);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(sender);
            when(messageRepository.findById(999L)).thenReturn(Optional.of(replyMessage));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> messageService.create(request));

            assertThat(ex.getMessage()).isEqualTo("Reply message not belong conversation");
        }
    }

    @Test
    void create_invalidFileMediaType_throwsInvalidMediaException() {
        MultipartFile badFile = mock(MultipartFile.class);
        when(badFile.isEmpty()).thenReturn(false);
        when(badFile.getOriginalFilename()).thenReturn("malicious.exe");

        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Check this out");
        request.setFiles(List.of(badFile));

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        User sender = User.builder().id(USER_ID).username("alice").build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(true);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(sender);

            assertThrows(InvalidMediaException.class,
                    () -> messageService.create(request));
        }
    }

    @Test
    void create_success_withImageFileAndReply() {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        when(imageFile.getOriginalFilename()).thenReturn("photo.png");

        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Here is the picture");
        request.setReplyToMessageId(200L);
        request.setFiles(List.of(imageFile));

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        Message replyMessage = Message.builder().id(200L).conversation(conversation).build();
        User sender = User.builder().id(USER_ID).username("alice").build();
        User receiver = User.builder().id(2L).username("bob").build();

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/photo.png")
                .publicId("photo-pub-id")
                .build();

        OffsetDateTime now = OffsetDateTime.now();
        Message savedMessage = Message.builder()
                .id(MESSAGE_ID)
                .conversation(conversation)
                .sender(sender)
                .content("Here is the picture")
                .replyToMessage(replyMessage)
                .createdAt(now)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(true);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(sender);
            when(messageRepository.findById(200L)).thenReturn(Optional.of(replyMessage));
            when(cloudinaryService.uploadFile(imageFile, "conversation/message", MediaType.IMAGE))
                    .thenReturn(uploadResponse);
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
            when(conversationMemberRepository.findOtherMembers(CONVERSATION_ID, USER_ID))
                    .thenReturn(List.of(receiver));

            MessageResponse response = messageService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(MESSAGE_ID);
            assertThat(response.getContent()).isEqualTo("Here is the picture");
            assertThat(response.getReplyToMessageId()).isEqualTo(200L);
            assertThat(response.getSenderId()).isEqualTo(USER_ID);

            verify(messageRepository).save(any(Message.class));
            verify(conversationRepository).save(conversation);
            verify(messagePublisher).sendToUser(eq(2L), any(MessageResponse.class));
        }
    }

    // ---------------------------------------------------------------------------
    // getMessages
    // ---------------------------------------------------------------------------

    @Test
    void getMessages_notMember_throwsAccessDeniedException() {
        Pageable pageable = PageRequest.of(0, 10);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(false);

            assertThrows(AccessDeniedException.class,
                    () -> messageService.getMessages(CONVERSATION_ID, pageable));
        }
    }

    @Test
    void getMessages_success_mapsMediaAndReactions() {
        Pageable pageable = PageRequest.of(0, 10);

        MessageProjection proj = mock(MessageProjection.class);
        when(proj.getId()).thenReturn(MESSAGE_ID);
        when(proj.getSenderId()).thenReturn(USER_ID);
        when(proj.getSenderName()).thenReturn("alice");
        when(proj.getContent()).thenReturn("Hello");
        when(proj.getReplyToMessageId()).thenReturn(null);
        when(proj.getCreatedAt()).thenReturn(OffsetDateTime.now());

        Page<MessageProjection> messagePage = new PageImpl<>(List.of(proj));

        Message mockMessage = Message.builder().id(MESSAGE_ID).build();
        MessageMedia media = MessageMedia.builder()
                .id(1L)
                .message(mockMessage)
                .url("https://example.com/img.png")
                .mediaType(MediaType.IMAGE)
                .build();

        User reactingUser = User.builder().id(USER_ID).build();
        MessageReaction reaction = MessageReaction.builder()
                .id(1L)
                .message(mockMessage)
                .user(reactingUser)
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(true);
            when(messageRepository.findMessages(CONVERSATION_ID, pageable)).thenReturn(messagePage);
            when(messageMediaRepository.findByMessageIds(List.of(MESSAGE_ID))).thenReturn(List.of(media));
            when(messageReactionRepository.findByMessageIds(List.of(MESSAGE_ID))).thenReturn(List.of(reaction));

            Page<MessageResponse> result = messageService.getMessages(CONVERSATION_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            MessageResponse resp = result.getContent().get(0);
            assertThat(resp.getId()).isEqualTo(MESSAGE_ID);
            assertThat(resp.getContent()).isEqualTo("Hello");
            assertThat(resp.getMedias()).hasSize(1);
            assertThat(resp.getMyReaction()).isEqualTo(ReactionType.LIKE);
            assertThat(resp.getTotalReactions()).isEqualTo(1L);
        }
    }

    // ---------------------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------------------

    @Test
    void delete_messageNotFound_throwsMessageNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findByIdWithSenderAndMedia(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThrows(MessageNotFoundException.class,
                    () -> messageService.delete(MESSAGE_ID));
        }
    }

    @Test
    void delete_notSender_throwsAccessDeniedException() {
        User sender = User.builder().id(999L).build(); // different sender
        Message message = Message.builder()
                .id(MESSAGE_ID)
                .sender(sender)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findByIdWithSenderAndMedia(MESSAGE_ID)).thenReturn(Optional.of(message));

            assertThrows(AccessDeniedException.class,
                    () -> messageService.delete(MESSAGE_ID));
        }
    }

    @Test
    void delete_success_deletesMediaAndUpdatesLastMessage() {
        User sender = User.builder().id(USER_ID).build();
        MessageMedia media = MessageMedia.builder()
                .publicId("media-pub-id")
                .mediaType(MediaType.IMAGE)
                .build();

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        Message messageToDelete = Message.builder()
                .id(MESSAGE_ID)
                .sender(sender)
                .conversation(conversation)
                .media(new ArrayList<>(List.of(media)))
                .build();

        conversation.setLastMessage(messageToDelete);

        OffsetDateTime prevTime = OffsetDateTime.now().minusMinutes(5);
        Message previousMessage = Message.builder()
                .id(99L)
                .createdAt(prevTime)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findByIdWithSenderAndMedia(MESSAGE_ID)).thenReturn(Optional.of(messageToDelete));
            when(messageRepository.findTopByConversationIdAndIdLessThanAndDeletedFalseOrderByIdDesc(CONVERSATION_ID, MESSAGE_ID))
                    .thenReturn(Optional.of(previousMessage));

            messageService.delete(MESSAGE_ID);

            verify(conversationDomainService).checkOwner(messageToDelete);
            verify(cloudinaryService).deleteFile("media-pub-id", MediaType.IMAGE);
            assertThat(conversation.getLastMessage()).isEqualTo(previousMessage);
            assertThat(conversation.getLastMessageAt()).isEqualTo(prevTime);
            assertThat(messageToDelete.getDeleted()).isTrue();
        }
    }

    @Test
    void delete_success_whenNoPreviousMessage_setsLastMessageNull() {
        User sender = User.builder().id(USER_ID).build();
        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        Message messageToDelete = Message.builder()
                .id(MESSAGE_ID)
                .sender(sender)
                .conversation(conversation)
                .media(new ArrayList<>())
                .build();

        conversation.setLastMessage(messageToDelete);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findByIdWithSenderAndMedia(MESSAGE_ID)).thenReturn(Optional.of(messageToDelete));
            when(messageRepository.findTopByConversationIdAndIdLessThanAndDeletedFalseOrderByIdDesc(CONVERSATION_ID, MESSAGE_ID))
                    .thenReturn(Optional.empty());

            messageService.delete(MESSAGE_ID);

            assertThat(conversation.getLastMessage()).isNull();
            assertThat(conversation.getLastMessageAt()).isNull();
            assertThat(messageToDelete.getDeleted()).isTrue();
        }
    }

    @Test
    void create_success_withVideoFile() {
        MultipartFile videoFile = mock(MultipartFile.class);
        when(videoFile.isEmpty()).thenReturn(false);
        when(videoFile.getOriginalFilename()).thenReturn("video.mp4");

        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Video message");
        request.setFiles(List.of(videoFile));

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        User sender = User.builder().id(USER_ID).username("alice").build();
        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/video.mp4")
                .publicId("video-pub-id")
                .build();

        Message savedMessage = Message.builder()
                .id(MESSAGE_ID)
                .conversation(conversation)
                .sender(sender)
                .content("Video message")
                .createdAt(OffsetDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(sender);
            when(cloudinaryService.uploadFile(videoFile, "conversation/message", MediaType.VIDEO))
                    .thenReturn(uploadResponse);
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
            when(conversationMemberRepository.findOtherMembers(CONVERSATION_ID, USER_ID))
                    .thenReturn(Collections.emptyList());

            MessageResponse response = messageService.create(request);

            assertThat(response).isNotNull();
            verify(cloudinaryService).uploadFile(videoFile, "conversation/message", MediaType.VIDEO);
        }
    }

    @Test
    void create_success_textOnly_noFiles() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Simple text message");
        request.setFiles(null);

        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).build();
        User sender = User.builder().id(USER_ID).username("alice").build();

        Message savedMessage = Message.builder()
                .id(MESSAGE_ID)
                .conversation(conversation)
                .sender(sender)
                .content("Simple text message")
                .createdAt(OffsetDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(sender);
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
            when(conversationMemberRepository.findOtherMembers(CONVERSATION_ID, USER_ID))
                    .thenReturn(Collections.emptyList());

            MessageResponse response = messageService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("Simple text message");
            verify(cloudinaryService, never()).uploadFile(any(), any(), any());
        }
    }

    @Test
    void delete_success_whenNotLastMessage_doesNotChangeConversationLastMessage() {
        User sender = User.builder().id(USER_ID).build();
        Message otherLastMessage = Message.builder().id(999L).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .lastMessage(otherLastMessage)
                .build();

        Message messageToDelete = Message.builder()
                .id(MESSAGE_ID)
                .sender(sender)
                .conversation(conversation)
                .media(new ArrayList<>())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(messageRepository.findByIdWithSenderAndMedia(MESSAGE_ID)).thenReturn(Optional.of(messageToDelete));

            messageService.delete(MESSAGE_ID);

            verify(messageRepository, never()).findTopByConversationIdAndIdLessThanAndDeletedFalseOrderByIdDesc(any(), any());
            assertThat(conversation.getLastMessage()).isEqualTo(otherLastMessage);
            assertThat(messageToDelete.getDeleted()).isTrue();
        }
    }
}
