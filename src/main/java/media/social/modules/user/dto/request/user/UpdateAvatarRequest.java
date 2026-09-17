package media.social.modules.user.dto.request.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
public class UpdateAvatarRequest {

    private MultipartFile file;
}
