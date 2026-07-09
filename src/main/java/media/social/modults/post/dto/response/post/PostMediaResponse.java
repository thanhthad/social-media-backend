package media.social.modults.post.dto.response.post;

import lombok.*;
import media.social.modults.post.enums.MediaType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostMediaResponse {
    private String url;
    private String publicId;
    private MediaType type;
}