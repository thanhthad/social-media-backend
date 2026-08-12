package media.social.modules.story.dto.response;

import lombok.*;
import media.social.modules.post.enums.MediaType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyStoryResponse {

    private Long storyId;

    private Long userId;

    private String username;

    private String avatarUrl;

    private String content;

    private String visibility;

    private MediaType mediaType;

    private String url;

    private List<StoryViewAndReaction> interactions;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
