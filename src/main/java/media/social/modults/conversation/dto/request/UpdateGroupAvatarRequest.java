package media.social.modults.conversation.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UpdateGroupAvatarRequest {

    private MultipartFile file;

}