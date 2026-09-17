package media.social.modules.conversation.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.conversation.dto.projection.ConversationListProjection;
import media.social.modules.conversation.dto.projection.ConversationMemberProjection;
import media.social.modules.conversation.dto.projection.DatingConversationListProjection;
import media.social.modules.conversation.dto.projection.UnreadCountProjection;
import media.social.modules.conversation.dto.request.CreateGroupRequest;
import media.social.modules.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modules.conversation.dto.response.ConversationListResponse;
import media.social.modules.conversation.dto.response.ConversationResponse;
import media.social.modules.conversation.dto.response.DatingConversationListResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.exception.ConversationNotFoundException;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.conversation.service.domain.ConversationDomainService;
import media.social.modules.conversation.websocket.ConversationPublisher;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.enums.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

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
class ConversationServiceImplTest {

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationDomainService conversationService;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private MediaUploadService mediaUploadService;

    @Mock
    private ConversationPublisher conversationPublisher;

    @InjectMocks
    private ConversationServiceImpl conversationServiceImpl;

    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;
    private static final Long CONVERSATION_ID = 100L;

    // ---------------------------------------------------------------------------
    // createPrivateConversation
    // ---------------------------------------------------------------------------

    @Test
    void createPrivateConversation_sameUser_throwsIllegalArgumentException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> conversationServiceImpl.createPrivateConversation(USER_ID));

            assertThat(ex.getMessage()).isEqualTo("Cannot create conversation with yourself");
        }
    }

    @Test
    void createPrivateConversation_existingConversation_returnsMappedWithoutCreating() {
        User currentUser = User.builder().id(USER_ID).username("alice").build();
        User targetUser = User.builder().id(TARGET_USER_ID).username("bob").build();

        Conversation existing = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.PRIVATE)
                .build();

        ConversationMemberProjection m1 = mock(ConversationMemberProjection.class);
        when(m1.getUserId()).thenReturn(USER_ID);
        when(m1.getUsername()).thenReturn("alice");

        ConversationMemberProjection m2 = mock(ConversationMemberProjection.class);
        when(m2.getUserId()).thenReturn(TARGET_USER_ID);
        when(m2.getUsername()).thenReturn("bob");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(currentUser);
            when(userServiceDomain.getByUserId(TARGET_USER_ID)).thenReturn(targetUser);
            when(conversationMemberRepository.findPrivateConversation(USER_ID, TARGET_USER_ID))
                    .thenReturn(Optional.of(existing));
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                    .thenReturn(List.of(m1, m2));

            ConversationResponse response = conversationServiceImpl.createPrivateConversation(TARGET_USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(response.getDisplayName()).isEqualTo("bob");
            verify(conversationService, never()).create(any());
            verify(conversationPublisher, never()).sendToUser(any(), any());
        }
    }

    @Test
    void createPrivateConversation_newConversation_createsSavesAndPublishes() {
        User currentUser = User.builder().id(USER_ID).username("alice").build();
        User targetUser = User.builder().id(TARGET_USER_ID).username("bob").build();

        Conversation newConv = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.PRIVATE)
                .build();

        ConversationMemberProjection m1 = mock(ConversationMemberProjection.class);
        when(m1.getUserId()).thenReturn(USER_ID);
        when(m1.getUsername()).thenReturn("alice");

        ConversationMemberProjection m2 = mock(ConversationMemberProjection.class);
        when(m2.getUserId()).thenReturn(TARGET_USER_ID);
        when(m2.getUsername()).thenReturn("bob");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(currentUser);
            when(userServiceDomain.getByUserId(TARGET_USER_ID)).thenReturn(targetUser);
            when(conversationMemberRepository.findPrivateConversation(USER_ID, TARGET_USER_ID))
                    .thenReturn(Optional.empty());
            when(conversationService.create(ConversationType.PRIVATE)).thenReturn(newConv);
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                    .thenReturn(List.of(m1, m2));

            ConversationResponse response = conversationServiceImpl.createPrivateConversation(TARGET_USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(response.getDisplayName()).isEqualTo("bob");

            verify(conversationMemberRepository, times(2)).save(any(ConversationMember.class));
            verify(conversationPublisher).sendToUser(eq(TARGET_USER_ID), any(ConversationResponse.class));
        }
    }

    // ---------------------------------------------------------------------------
    // createGroupConversation
    // ---------------------------------------------------------------------------

    @Test
    void createGroupConversation_noMembers_throwsIllegalArgumentException() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("My Group");
        request.setMemberIds(Collections.emptyList());

        User currentUser = User.builder().id(USER_ID).username("alice").build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(currentUser);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> conversationServiceImpl.createGroupConversation(request)
            );

            assertThat(ex.getMessage()).isEqualTo("Group must have members");
        }
    }

    @Test
    void createGroupConversation_withAvatar_success() {
        MultipartFile avatarFile = mock(MultipartFile.class);
        when(avatarFile.isEmpty()).thenReturn(false);

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Dev Team");
        request.setMemberIds(List.of(2L, 3L));
        request.setAvatar(avatarFile);

        User currentUser = User.builder().id(USER_ID).username("alice").build();
        User user2 = User.builder().id(2L).username("bob").build();
        User user3 = User.builder().id(3L).username("charlie").build();

        Conversation newConv = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .build();

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/avatar.png")
                .publicId("pub-123")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(currentUser);
            when(userServiceDomain.getByUserId(2L)).thenReturn(user2);
            when(userServiceDomain.getByUserId(3L)).thenReturn(user3);
            when(conversationService.create(ConversationType.GROUP)).thenReturn(newConv);
            when(mediaUploadService.upload(avatarFile, MediaUploadContext.MESSAGE))
                    .thenReturn(uploadResponse);
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                    .thenReturn(Collections.emptyList());

            ConversationResponse response = conversationServiceImpl.createGroupConversation(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(response.getDisplayName()).isEqualTo("Dev Team");
            assertThat(response.getAvatarUrl()).isEqualTo("https://cloudinary.com/avatar.png");

            verify(mediaUploadService).upload(avatarFile, MediaUploadContext.MESSAGE);
            verify(conversationRepository).save(newConv);
            verify(conversationMemberRepository).save(any(ConversationMember.class)); // owner
            verify(conversationMemberRepository).saveAll(anyList()); // other members
            verify(conversationPublisher, times(2)).sendToUser(anyLong(), any(ConversationResponse.class));
        }
    }

    @Test
    void createGroupConversation_withoutAvatar_success() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Design Team");
        request.setMemberIds(List.of(2L));
        request.setAvatar(null);

        User currentUser = User.builder().id(USER_ID).username("alice").build();
        User user2 = User.builder().id(2L).username("bob").build();

        Conversation newConv = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(currentUser);
            when(userServiceDomain.getByUserId(2L)).thenReturn(user2);
            when(conversationService.create(ConversationType.GROUP)).thenReturn(newConv);
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                    .thenReturn(Collections.emptyList());

            ConversationResponse response = conversationServiceImpl.createGroupConversation(request);

            assertThat(response).isNotNull();
            assertThat(response.getDisplayName()).isEqualTo("Design Team");
            assertThat(response.getAvatarUrl()).isNull();

            verify(mediaUploadService, never()).upload(any(), any());
            verify(conversationRepository).save(newConv);
            verify(conversationMemberRepository).save(any(ConversationMember.class));
            verify(conversationMemberRepository).saveAll(anyList());
        }
    }

    @Test
    void createGroupConversation_filtersCurrentUserIdAndDuplicates() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Filtered Team");
        // memberIds contains USER_ID (owner) and duplicate 2L
        request.setMemberIds(List.of(USER_ID, 2L, 2L));
        request.setAvatar(null);

        User currentUser = User.builder().id(USER_ID).username("alice").build();
        User user2 = User.builder().id(2L).username("bob").build();

        Conversation newConv = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(currentUser);
            when(userServiceDomain.getByUserId(2L)).thenReturn(user2);
            when(conversationService.create(ConversationType.GROUP)).thenReturn(newConv);
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                    .thenReturn(Collections.emptyList());

            conversationServiceImpl.createGroupConversation(request);

            // Verify getByUserId called only once for user2 (USER_ID is filtered out, duplicate 2L is deduped)
            verify(userServiceDomain, times(1)).getByUserId(2L);
            ArgumentCaptor<List<ConversationMember>> captor = ArgumentCaptor.forClass(List.class);
            verify(conversationMemberRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }
    }

    // ---------------------------------------------------------------------------
    // updateGroupAvatar
    // ---------------------------------------------------------------------------

    @Test
    void updateGroupAvatar_notFound_throwsConversationNotFoundException() {
        MultipartFile file = mock(MultipartFile.class);

        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> conversationServiceImpl.updateGroupAvatar(CONVERSATION_ID, file));
    }

    @Test
    void updateGroupAvatar_notGroup_throwsIllegalArgumentException() {
        MultipartFile file = mock(MultipartFile.class);
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.PRIVATE)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(IllegalArgumentException.class,
                    () -> conversationServiceImpl.updateGroupAvatar(CONVERSATION_ID, file));
        }
    }

    @Test
    void updateGroupAvatar_notOwner_throwsAccessDeniedException() {
        MultipartFile file = mock(MultipartFile.class);
        User owner = User.builder().id(999L).build(); // different owner
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(AccessDeniedException.class,
                    () -> conversationServiceImpl.updateGroupAvatar(CONVERSATION_ID, file));
        }
    }

    @Test
    void updateGroupAvatar_success_deletesOldAvatarIfExists() {
        MultipartFile file = mock(MultipartFile.class);
        User owner = User.builder().id(USER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .avatarPublicId("old-pub-id")
                .build();

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/new.png")
                .publicId("new-pub-id")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(mediaUploadService.upload(file, MediaUploadContext.MESSAGE)).thenReturn(uploadResponse);

            conversationServiceImpl.updateGroupAvatar(CONVERSATION_ID, file);

            verify(mediaUploadService).delete("old-pub-id", MediaType.IMAGE);
            assertThat(conversation.getAvatarUrl()).isEqualTo("https://cloudinary.com/new.png");
            assertThat(conversation.getAvatarPublicId()).isEqualTo("new-pub-id");
        }
    }

    // ---------------------------------------------------------------------------
    // updateGroupName
    // ---------------------------------------------------------------------------

    @Test
    void updateGroupName_notFound_throwsConversationNotFoundException() {
        UpdateGroupNameRequest req = new UpdateGroupNameRequest();
        req.setName("New Name");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> conversationServiceImpl.updateGroupName(CONVERSATION_ID, req));
        }
    }

    @Test
    void updateGroupName_notGroup_throwsIllegalArgumentException() {
        UpdateGroupNameRequest req = new UpdateGroupNameRequest();
        req.setName("New Name");
        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).type(ConversationType.PRIVATE).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(IllegalArgumentException.class,
                    () -> conversationServiceImpl.updateGroupName(CONVERSATION_ID, req));
        }
    }

    @Test
    void updateGroupName_notOwner_throwsAccessDeniedException() {
        UpdateGroupNameRequest req = new UpdateGroupNameRequest();
        req.setName("New Name");
        User owner = User.builder().id(999L).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(AccessDeniedException.class,
                    () -> conversationServiceImpl.updateGroupName(CONVERSATION_ID, req));
        }
    }

    @Test
    void updateGroupName_success() {
        UpdateGroupNameRequest req = new UpdateGroupNameRequest();
        req.setName("Updated Name");
        User owner = User.builder().id(USER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .name("Old Name")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            conversationServiceImpl.updateGroupName(CONVERSATION_ID, req);

            assertThat(conversation.getName()).isEqualTo("Updated Name");
        }
    }

    // ---------------------------------------------------------------------------
    // deleteConversation
    // ---------------------------------------------------------------------------

    @Test
    void deleteConversation_notFound_throwsConversationNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> conversationServiceImpl.deleteConversation(CONVERSATION_ID));
        }
    }

    @Test
    void deleteConversation_notGroup_throwsIllegalArgumentException() {
        Conversation conversation = Conversation.builder().id(CONVERSATION_ID).type(ConversationType.PRIVATE).build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(IllegalArgumentException.class,
                    () -> conversationServiceImpl.deleteConversation(CONVERSATION_ID));
        }
    }

    @Test
    void deleteConversation_notOwner_throwsAccessDeniedException() {
        User owner = User.builder().id(999L).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            assertThrows(AccessDeniedException.class,
                    () -> conversationServiceImpl.deleteConversation(CONVERSATION_ID));
        }
    }

    @Test
    void deleteConversation_success_deletesAvatarAndConversation() {
        User owner = User.builder().id(USER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .avatarPublicId("avatar-id")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            conversationServiceImpl.deleteConversation(CONVERSATION_ID);

            verify(mediaUploadService).delete("avatar-id", MediaType.IMAGE);
            verify(conversationRepository).delete(conversation);
        }
    }

    @Test
    void deleteConversation_withoutAvatar_success() {
        User owner = User.builder().id(USER_ID).build();
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .owner(owner)
                .avatarPublicId(null)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findByIdWithOwner(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

            conversationServiceImpl.deleteConversation(CONVERSATION_ID);

            verify(mediaUploadService, never()).delete(any(), any());
            verify(conversationRepository).delete(conversation);
        }
    }

    // ---------------------------------------------------------------------------
    // getConversationDetail
    // ---------------------------------------------------------------------------

    @Test
    void getConversationDetail_notMember_throwsAccessDeniedException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(false);

            assertThrows(AccessDeniedException.class,
                    () -> conversationServiceImpl.getConversationDetail(CONVERSATION_ID));
        }
    }

    @Test
    void getConversationDetail_notFound_throwsConversationNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> conversationServiceImpl.getConversationDetail(CONVERSATION_ID));
        }
    }

    @Test
    void getConversationDetail_success() {
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.GROUP)
                .name("General")
                .avatarUrl("https://example.com/group.png")
                .build();

        ConversationMemberProjection m = mock(ConversationMemberProjection.class);
        when(m.getUserId()).thenReturn(USER_ID);
        when(m.getUsername()).thenReturn("alice");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID)).thenReturn(List.of(m));

            ConversationResponse response = conversationServiceImpl.getConversationDetail(CONVERSATION_ID);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(response.getDisplayName()).isEqualTo("General");
            assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/group.png");
        }
    }

    @Test
    void getConversationDetail_privateConversation_success() {
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .type(ConversationType.PRIVATE)
                .build();

        ConversationMemberProjection mCurrent = mock(ConversationMemberProjection.class);
        when(mCurrent.getUserId()).thenReturn(USER_ID);
        when(mCurrent.getUsername()).thenReturn("alice");
        when(mCurrent.getAvatarUrl()).thenReturn("https://example.com/alice.png");

        ConversationMemberProjection mTarget = mock(ConversationMemberProjection.class);
        when(mTarget.getUserId()).thenReturn(TARGET_USER_ID);
        when(mTarget.getUsername()).thenReturn("bob");
        when(mTarget.getAvatarUrl()).thenReturn("https://example.com/bob.png");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationMemberRepository.existsByConversationIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(true);
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            when(conversationMemberRepository.findMembersProjectionByConversationId(CONVERSATION_ID))
                    .thenReturn(List.of(mCurrent, mTarget));

            ConversationResponse response = conversationServiceImpl.getConversationDetail(CONVERSATION_ID);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CONVERSATION_ID);
            assertThat(response.getDisplayName()).isEqualTo("bob");
            assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/bob.png");
        }
    }

    // ---------------------------------------------------------------------------
    // getMyConversations
    // ---------------------------------------------------------------------------

    @Test
    void getMyConversations_empty_returnsEmptyList() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findConversationList(USER_ID, ConversationType.PRIVATE, ConversationType.DATING))
                    .thenReturn(Collections.emptyList());

            List<ConversationListResponse> result = conversationServiceImpl.getMyConversations();

            assertThat(result).isEmpty();
        }
    }

    @Test
    void getMyConversations_success_withUnreadCounts() {
        ConversationListProjection proj = mock(ConversationListProjection.class);
        when(proj.getConversationId()).thenReturn(CONVERSATION_ID);
        when(proj.getUserId()).thenReturn(TARGET_USER_ID);
        when(proj.getType()).thenReturn(ConversationType.PRIVATE);
        when(proj.getDisplayName()).thenReturn("Bob");
        when(proj.getAvatarUrl()).thenReturn("https://example.com/bob.png");
        when(proj.getPreview()).thenReturn("Hey there");
        when(proj.getLastMessageAt()).thenReturn(OffsetDateTime.now());

        UnreadCountProjection unreadProj = mock(UnreadCountProjection.class);
        when(unreadProj.getConversationId()).thenReturn(CONVERSATION_ID);
        when(unreadProj.getUnreadCount()).thenReturn(5L);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findConversationList(USER_ID, ConversationType.PRIVATE, ConversationType.DATING))
                    .thenReturn(List.of(proj));
            when(conversationMemberRepository.countUnreadMessagesByConversationIds(USER_ID, List.of(CONVERSATION_ID)))
                    .thenReturn(List.of(unreadProj));

            List<ConversationListResponse> result = conversationServiceImpl.getMyConversations();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConversation_id()).isEqualTo(CONVERSATION_ID);
            assertThat(result.get(0).getDisplayName()).isEqualTo("Bob");
            assertThat(result.get(0).getUnreadCount()).isEqualTo(5L);
        }
    }

    // ---------------------------------------------------------------------------
    // getMyDatingConversations
    // ---------------------------------------------------------------------------

    @Test
    void getMyDatingConversations_empty_returnsEmptyList() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findDatingConversationList(USER_ID, ConversationType.DATING))
                    .thenReturn(Collections.emptyList());

            List<DatingConversationListResponse> result = conversationServiceImpl.getMyDatingConversations();

            assertThat(result).isEmpty();
        }
    }

    @Test
    void getMyDatingConversations_success_withUnreadCounts() {
        DatingConversationListProjection proj = mock(DatingConversationListProjection.class);
        when(proj.getConversationId()).thenReturn(CONVERSATION_ID);
        when(proj.getDatingProfileId()).thenReturn(50L);
        when(proj.getType()).thenReturn(ConversationType.DATING);
        when(proj.getDisplayName()).thenReturn("Alice Dating");
        when(proj.getAvatarUrl()).thenReturn("https://example.com/dating.png");
        when(proj.getPreview()).thenReturn("Match message");
        when(proj.getLastMessageAt()).thenReturn(OffsetDateTime.now());

        UnreadCountProjection unreadProj = mock(UnreadCountProjection.class);
        when(unreadProj.getConversationId()).thenReturn(CONVERSATION_ID);
        when(unreadProj.getUnreadCount()).thenReturn(2L);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(conversationRepository.findDatingConversationList(USER_ID, ConversationType.DATING))
                    .thenReturn(List.of(proj));
            when(conversationMemberRepository.countUnreadMessagesByConversationIds(USER_ID, List.of(CONVERSATION_ID)))
                    .thenReturn(List.of(unreadProj));

            List<DatingConversationListResponse> result = conversationServiceImpl.getMyDatingConversations();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConversation_id()).isEqualTo(CONVERSATION_ID);
            assertThat(result.get(0).getDatingProfile_id()).isEqualTo(50L);
            assertThat(result.get(0).getDisplayName()).isEqualTo("Alice Dating");
            assertThat(result.get(0).getUnreadCount()).isEqualTo(2L);
        }
    }
}
