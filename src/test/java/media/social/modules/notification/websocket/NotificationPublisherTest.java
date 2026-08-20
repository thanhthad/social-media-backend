package media.social.modules.notification.websocket;

import media.social.modules.notification.dto.response.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @InjectMocks
    private NotificationPublisher notificationPublisher;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendToUser_invokesConvertAndSendToUser() {
        Long receiverId = 123L;
        NotificationResponse response = NotificationResponse.builder()
                .notificationId(999L)
                .build();

        notificationPublisher.sendToUser(receiverId, response);

        verify(messagingTemplate).convertAndSendToUser(
                eq("123"),
                eq("/queue/notifications"),
                eq(response)
        );
    }

    @Test
    void sendGlobalNotification_invokesConvertAndSend() {
        String message = "System update in 5 minutes";

        notificationPublisher.sendGlobalNotification(message);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/system"),
                eq(message)
        );
    }
}
