package media.social.modules.post.dto.response.reaction;

import lombok.*;
import media.social.modules.post.enums.ReactionType;

import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReactionCountResponse {

    private Map<ReactionType, Long> counts;

}