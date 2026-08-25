package media.social.modules.reel.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import media.social.modules.post.enums.Visibility;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReelContentRequest {

    @Size(max = 1000, message = "Content max 1000 characters")
    private String content;

    private Visibility visibility;
}
