package media.social.modules.story.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import media.social.modules.post.enums.Visibility;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoryVisibilityRequest {

    @NotNull(message = "Visibility must not be null")
    private Visibility visibility;
}