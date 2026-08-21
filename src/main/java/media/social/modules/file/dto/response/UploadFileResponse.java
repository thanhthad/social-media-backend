package media.social.modules.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class UploadFileResponse {
    private String fileUrl;
    private String publicId;
    private String resourceType;
}