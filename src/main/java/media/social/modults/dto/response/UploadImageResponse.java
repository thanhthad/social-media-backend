package media.social.modults.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UploadImageResponse {

    private String imageUrl;

    private String publicId;
}