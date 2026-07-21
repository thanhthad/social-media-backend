package media.social.modults.conversation.dto.request;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreateMessageRequest {

    private Long conversationId;

    private String content;

    private Long replyToMessageId;
}