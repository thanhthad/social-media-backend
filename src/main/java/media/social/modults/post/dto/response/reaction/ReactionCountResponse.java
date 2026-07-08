package media.social.modults.post.dto.response.reaction;

import lombok.*;
import media.social.modults.post.enums.ReactionType;

import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReactionCountResponse {

    private Map<ReactionType, Long> counts;

}