package media.social.modules.conversation.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class CreateGroupRequest {

    private String name;

    private MultipartFile avatar;

    private List<Long> memberIds;
}