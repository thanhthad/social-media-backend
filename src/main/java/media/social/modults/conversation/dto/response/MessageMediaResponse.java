package media.social.modults.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageMediaResponse {

    private Long id;

    private String url;

    private String mediaType;

}