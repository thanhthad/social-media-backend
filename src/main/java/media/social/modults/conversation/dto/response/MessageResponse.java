package media.social.modults.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class MessageResponse {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private String senderName;

    private String content;

    private Long replyToMessageId;

    private List<MessageMediaResponse> medias;

    private OffsetDateTime createdAt;

    private boolean deleted;

}