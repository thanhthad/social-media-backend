package media.social.modults.conversation.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modults.conversation.dto.response.ConversationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationPublisher {

    private final SimpMessagingTemplate messagingTemplate;


    public void sendToUser(
            Long userId,
            ConversationResponse response
    ){

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/conversations",
                response
        );

    }

}