package media.social.modules.post.dto.request.post;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import media.social.modules.post.enums.Visibility;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostVisibility {

    @NotNull(message = "Visibility is required")
    private Visibility visibility;
}