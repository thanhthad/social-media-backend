package media.social.modults.conversation.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateMessageRequest {

    private String content;

    private List<Long> deletedMediaIds;

}