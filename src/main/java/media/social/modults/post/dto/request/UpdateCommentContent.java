package media.social.modults.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentContent {

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 1000, message = "Content max 1000 characters")
    private String content;
}
