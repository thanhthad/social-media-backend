package media.social.modules.reel.dto.request;

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
public class CreateReelRequest {

    @Size(max = 1000, message = "Content max 1000 characters")
    private String content;

    @NotNull(message = "Visibility is required")
    private Visibility visibility;

    @NotNull(message = "Video file is required")
    private MultipartFile video;

    private MultipartFile thumbnail;
}