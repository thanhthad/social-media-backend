package media.social.modules.post.dto.request.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import media.social.modules.post.enums.Visibility;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostContent {

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 1000, message = "Content max 1000 characters")
    private String content;

    @NotNull
    private Visibility visibility;


}