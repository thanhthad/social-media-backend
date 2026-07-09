package media.social.modults.post.dto.response.post;

import lombok.AllArgsConstructor;
import lombok.Getter;
import media.social.modults.post.enums.Visibility;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostFlatResponse {

    private Long id;
    private String content;
    private Visibility visibility;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String avatarUrl;
}