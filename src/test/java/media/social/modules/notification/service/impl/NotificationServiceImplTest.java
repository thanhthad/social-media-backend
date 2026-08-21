package media.social.modules.notification.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.notification.dto.projection.NotificationProjection;
import media.social.modules.notification.dto.response.NotificationResponse;
import media.social.modules.notification.entity.Notification;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.exception.NotificationNotFoundException;
import media.social.modules.notification.repository.NotificationRepository;
import media.social.modules.notification.websocket.NotificationPublisher;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    // =========================================================================
    // create()
    // =========================================================================

    @Test
    void create_SenderAndReceiverSame_DoesNothing() {
        User user = User.builder().id(1L).build();

        notificationService.create(user, user, EntityType.POST, 100L, NotificationType.POST_REACTION);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    void create_NewNotification_SavesAndPublishes() {
        User receiver = User.builder().id(1L).build();
        Profile senderProfile = Profile.builder().avatarUrl("http://sender.avatar").build();
        User sender = User.builder()
                .id(2L)
                .username("sender_username")
                .profile(senderProfile)
                .build();

        EntityType entityType = EntityType.POST;
        Long entityId = 100L;
        NotificationType type = NotificationType.POST_REACTION;

        when(notificationRepository.findByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                1L, 2L, entityType, entityId, type))
                .thenReturn(Optional.empty());

        Notification savedNotification = Notification.builder()
                .id(500L)
                .receiver(receiver)
                .sender(sender)
                .entityType(entityType)
                .entityId(entityId)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.create(receiver, sender, entityType, entityId, type);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();
        assertEquals(receiver, captured.getReceiver());
        assertEquals(sender, captured.getSender());
        assertEquals(entityType, captured.getEntityType());
        assertEquals(entityId, captured.getEntityId());
        assertEquals(type, captured.getType());
        assertFalse(captured.getIsRead());
        assertNotNull(captured.getCreatedAt());

        ArgumentCaptor<NotificationResponse> responseCaptor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(notificationPublisher).sendToUser(eq(1L), responseCaptor.capture());
        NotificationResponse response = responseCaptor.getValue();
        assertEquals(500L, response.getNotificationId());
        assertEquals(2L, response.getSenderId());
        assertEquals("sender_username", response.getSenderUsername());
        assertEquals("http://sender.avatar", response.getSenderAvatar());
        assertEquals(type, response.getType());
        assertEquals(entityType, response.getEntityType());
        assertEquals(entityId, response.getEntityId());
        assertFalse(response.isRead());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void create_ExistingNotification_UpdatesAndPublishes() {
        User receiver = User.builder().id(1L).build();
        Profile senderProfile = Profile.builder().avatarUrl("http://sender.avatar").build();
        User sender = User.builder()
                .id(2L)
                .username("sender_username")
                .profile(senderProfile)
                .build();

        EntityType entityType = EntityType.POST;
        Long entityId = 100L;
        NotificationType type = NotificationType.POST_REACTION;
        LocalDateTime initialTime = LocalDateTime.now().minusDays(1);

        Notification existingNotification = Notification.builder()
                .id(500L)
                .receiver(receiver)
                .sender(sender)
                .entityType(entityType)
                .entityId(entityId)
                .type(type)
                .isRead(true)
                .createdAt(initialTime)
                .build();

        when(notificationRepository.findByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                1L, 2L, entityType, entityId, type))
                .thenReturn(Optional.of(existingNotification));

        when(notificationRepository.save(any(Notification.class))).thenReturn(existingNotification);

        notificationService.create(receiver, sender, entityType, entityId, type);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();
        assertEquals(500L, captured.getId());
        assertFalse(captured.getIsRead());
        assertTrue(captured.getCreatedAt().isAfter(initialTime));

        verify(notificationPublisher).sendToUser(eq(1L), any(NotificationResponse.class));
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Test
    void delete_NotExists_DoesNothing() {
        Long receiverId = 1L;
        Long senderId = 2L;
        EntityType entityType = EntityType.POST;
        Long entityId = 100L;
        NotificationType type = NotificationType.POST_REACTION;

        when(notificationRepository.existsByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                receiverId, senderId, entityType, entityId, type))
                .thenReturn(false);

        notificationService.delete(receiverId, senderId, entityType, entityId, type);

        verify(notificationRepository, never()).deleteByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                anyLong(), anyLong(), any(EntityType.class), anyLong(), any(NotificationType.class));
    }

    @Test
    void delete_Exists_Deletes() {
        Long receiverId = 1L;
        Long senderId = 2L;
        EntityType entityType = EntityType.POST;
        Long entityId = 100L;
        NotificationType type = NotificationType.POST_REACTION;

        when(notificationRepository.existsByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                receiverId, senderId, entityType, entityId, type))
                .thenReturn(true);

        notificationService.delete(receiverId, senderId, entityType, entityId, type);

        verify(notificationRepository).deleteByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                receiverId, senderId, entityType, entityId, type);
    }

    // =========================================================================
    // getMyNotifications()
    // =========================================================================

    @Test
    void getMyNotifications_ReturnsPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        NotificationProjection mockProjection = mock(NotificationProjection.class);
        when(mockProjection.getNotificationId()).thenReturn(500L);
        when(mockProjection.getSenderUsername()).thenReturn("sender");
        Page<NotificationProjection> mockPage = new PageImpl<>(List.of(mockProjection));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(notificationRepository.findMyNotifications(userId, pageable)).thenReturn(mockPage);

            Page<NotificationResponse> result = notificationService.getMyNotifications(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("sender", result.getContent().get(0).getSenderUsername());
            assertEquals(500L, result.getContent().get(0).getNotificationId());
            verify(notificationRepository).findMyNotifications(userId, pageable);
        }
    }

    // =========================================================================
    // markAsRead()
    // =========================================================================

    @Test
    void markAsRead_Success() {
        Long userId = 1L;
        Long notificationId = 500L;
        Notification mockNotification = Notification.builder()
                .id(notificationId)
                .isRead(false)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(notificationRepository.findByIdAndReceiver_Id(notificationId, userId))
                    .thenReturn(Optional.of(mockNotification));

            notificationService.markAsRead(notificationId);

            assertTrue(mockNotification.getIsRead());
            verify(notificationRepository).findByIdAndReceiver_Id(notificationId, userId);
        }
    }

    @Test
    void markAsRead_NotFound_ThrowsNotificationNotFoundException() {
        Long userId = 1L;
        Long notificationId = 500L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(notificationRepository.findByIdAndReceiver_Id(notificationId, userId))
                    .thenReturn(Optional.empty());

            assertThrows(NotificationNotFoundException.class, () ->
                    notificationService.markAsRead(notificationId)
            );

            verify(notificationRepository).findByIdAndReceiver_Id(notificationId, userId);
        }
    }

    // =========================================================================
    // markAllAsRead()
    // =========================================================================

    @Test
    void markAllAsRead_Success() {
        Long userId = 1L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            notificationService.markAllAsRead();

            verify(notificationRepository).markAllAsRead(userId);
        }
    }

    // =========================================================================
    // countUnread()
    // =========================================================================

    @Test
    void countUnread_ReturnsCount() {
        Long userId = 1L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(notificationRepository.countByReceiver_IdAndIsReadFalse(userId)).thenReturn(5L);

            long result = notificationService.countUnread();

            assertEquals(5L, result);
            verify(notificationRepository).countByReceiver_IdAndIsReadFalse(userId);
        }
    }
}
