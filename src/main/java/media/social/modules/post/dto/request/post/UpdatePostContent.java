package media.social.modules.post.dto.request.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostContent {

    @NotBlank(message = "Content is required")
    @Size(max = 1000, message = "Content max 1000 characters")
    private String content;
}