package media.social.modules.post.dto.request.post;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import media.social.modules.post.enums.Visibility;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {

    @Size(max = 1000, message = "Content max 1000 characters")
    private String content;

    @NotNull
    private Visibility visibility;

    private List<MultipartFile> files;

}
