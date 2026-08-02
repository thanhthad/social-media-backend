package media.social.modules.post.dto.request.reaction;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import media.social.modules.post.enums.ReactionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionRequest {

    @NotNull(message = "Reaction type must not be null")
    private ReactionType type;

}