package media.social.modules.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class LastMessageResponse {

    private Long id;

    private String preview;

    private Long senderId;

    private String senderName;

    private OffsetDateTime createdAt;
}