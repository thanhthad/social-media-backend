package media.social.modults.post.dto.request.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import media.social.modults.post.enums.Visibility;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @Size(max = 1000, message = "Content max 1000 characters")
    private String content;

    @NotNull
    private Visibility visibility;

    private List<MultipartFile> files;

}
