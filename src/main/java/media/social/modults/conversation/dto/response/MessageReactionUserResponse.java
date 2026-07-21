package media.social.modults.conversation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;


@Getter
@AllArgsConstructor
public class MessageReactionUserResponse {

    private Long userId;

    private String email;

    private String avatarUrl;

    private OffsetDateTime createdAt;

}