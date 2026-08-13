package media.social.modules.story.dto.response;

import lombok.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStoryResponse {

    private Long storyId;

    private Long userId;

    private String username;

    private String avatarUrl;

    private String content;

    private Visibility visibility;

    private MediaType mediaType;

    private String url;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}