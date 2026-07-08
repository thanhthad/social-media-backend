package media.social.modults.post.dto.request.reaction;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import media.social.modults.post.enums.ReactionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionRequest {

    @NotNull(message = "Reaction type must not be null")
    private ReactionType type;

}