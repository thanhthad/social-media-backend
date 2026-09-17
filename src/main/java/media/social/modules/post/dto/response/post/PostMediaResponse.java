package media.social.modules.post.dto.response.post;

import lombok.*;
import media.social.modules.post.enums.MediaType;

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