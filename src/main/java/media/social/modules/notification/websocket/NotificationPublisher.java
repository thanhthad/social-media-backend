package media.social.modules.notification.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modules.notification.dto.response.NotificationResponse;
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

    public void sendGlobalNotification(
            String message
    ){
        messagingTemplate.convertAndSend(
                "/topic/system",
                message
        );
    }

}