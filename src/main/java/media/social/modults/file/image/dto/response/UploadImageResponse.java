package media.social.modults.file.image.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class UploadImageResponse {

    private String imageUrl;

    private String publicId;
}