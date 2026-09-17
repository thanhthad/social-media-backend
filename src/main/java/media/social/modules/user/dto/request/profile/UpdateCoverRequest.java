package media.social.modules.user.dto.request.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


@Getter
@Setter
@AllArgsConstructor
public class UpdateCoverRequest {

    private MultipartFile file;

}