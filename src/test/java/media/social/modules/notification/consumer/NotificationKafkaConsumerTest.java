package media.social.modules.notification.consumer;

import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.event.NotificationEvent;
import media.social.modules.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

    @InjectMocks
    private NotificationKafkaConsumer notificationKafkaConsumer;

    @Mock
    private NotificationService notificationService;

    @Test
    void consumeNotification_Success() {
        NotificationEvent event = new NotificationEvent(
                1L,
                2L,
                EntityType.POST,
                100L,
                NotificationType.POST_REACTION,
                LocalDateTime.now()
        );

        notificationKafkaConsumer.consumeNotification(event);

        verify(notificationService).processNotificationEvent(event);
    }

    @Test
    void consumeNotification_WhenServiceThrows_ThrowsExceptionForRetry() {
        NotificationEvent event = new NotificationEvent(
                1L,
                2L,
                EntityType.POST,
                100L,
                NotificationType.POST_REACTION,
                LocalDateTime.now()
        );

        doThrow(new RuntimeException("DB Connection timeout"))
                .when(notificationService).processNotificationEvent(event);

        assertThrows(RuntimeException.class, () ->
                notificationKafkaConsumer.consumeNotification(event)
        );

        verify(notificationService).processNotificationEvent(event);
    }
}
