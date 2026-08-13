package media.social.modules.story.dto.response;

import lombok.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryFeedResponse {

    private Long userId;

    private String avatarUrl;

    private String content;

    private Visibility visibility;

    private MediaType mediaType;

    private String url;

}
