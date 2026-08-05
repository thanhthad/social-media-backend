package media.social.modules.conversation.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.dto.response.MessageResponse;
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