package media.social.modults.post.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostMediaResponse {
    private String url;
    private String publicId;
    private String type;
}