package media.social.modults.notification.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modults.notification.dto.response.NotificationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(
            Long receiverId,
            NotificationResponse response
    ) {

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/notifications",
                response
        );

    }

}