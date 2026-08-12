package media.social.modules.story.dto.response;

import lombok.*;
import media.social.modules.post.enums.MediaType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryFeedResponse {

    private Long userId;

    private String avatarUrl;

    private String content;

    private String visibility;

    private MediaType mediaType;

    private String url;

}
