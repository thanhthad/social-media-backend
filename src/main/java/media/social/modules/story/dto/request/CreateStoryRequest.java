package media.social.modules.story.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import media.social.modules.post.enums.Visibility;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoryRequest {

    @NotNull(message = "Story content must not be null")
    @Size(
            max = 500,
            message = "Story content must not exceed 500 characters"
    )
    private String content;

    @NotNull(message = "Visibility must not be null")
    private Visibility visibility;

    @NotNull(message = "Story file must not be null")
    private MultipartFile file;
}