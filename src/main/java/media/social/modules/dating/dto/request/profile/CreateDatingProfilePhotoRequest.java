package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDatingProfilePhotoRequest {

    @NotNull(message = "Photo file is required")
    private MultipartFile file;
}