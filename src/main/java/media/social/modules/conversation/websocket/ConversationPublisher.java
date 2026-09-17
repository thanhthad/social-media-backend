package media.social.modules.conversation.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.dto.response.ConversationResponse;
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