package media.social.modults.post.dto.response.reaction;

import lombok.*;
import media.social.modults.post.enums.ReactionType;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReactionResponse {

    private boolean reacted;

    private ReactionType type;

}