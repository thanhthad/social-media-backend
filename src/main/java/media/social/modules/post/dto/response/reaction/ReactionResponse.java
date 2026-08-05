package media.social.modules.post.dto.response.reaction;

import lombok.*;
import media.social.modules.post.enums.ReactionType;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReactionResponse {

    private boolean reacted;

    private ReactionType type;

}