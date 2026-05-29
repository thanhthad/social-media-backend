package media.social.modults.user.dto.request.self;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UpdateAvatarRequest {

    private MultipartFile file;
}
