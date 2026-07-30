package media.social.modults.conversation.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modults.conversation.dto.response.MessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;


    public void sendToUser(
            Long receiverId,
            MessageResponse response
    ) {

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                response
        );

    }

}