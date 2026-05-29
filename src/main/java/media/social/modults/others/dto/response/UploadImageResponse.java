package media.social.modults.others.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UploadImageResponse {

    private String imageUrl;

    private String publicId;
}