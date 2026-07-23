package media.social.modults.post.dto.response.post;

import lombok.*;
import media.social.modults.post.enums.MediaType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostMediaResponse {
    private Long postMediaId;
    private String url;
    private MediaType type;
}