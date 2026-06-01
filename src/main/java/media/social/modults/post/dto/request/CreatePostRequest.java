package media.social.modults.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

//    @NotNull(message = "UserId is required")
//    private Long userId;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 1000, message = "Content max 1000 characters")
    private String content;

    private List<MultipartFile> files;

}
