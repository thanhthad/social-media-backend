package media.social.modules.conversation.websocket;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.dto.response.MessageReactionResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageReactionPublisher {

    private final SimpMessagingTemplate messagingTemplate;


    public void send(
            Long userId,
            MessageReactionResponse response
    ){

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/message-reactions",
                response
        );

    }

}