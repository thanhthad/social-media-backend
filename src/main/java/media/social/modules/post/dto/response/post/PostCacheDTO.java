package media.social.modules.post.dto.response.post;

import lombok.*;
import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCacheDTO {

    private Long id;

    private String content;

    private Visibility visibility;

    private List<PostMediaResponse> postMediaResponses;

    private LocalDateTime createdAt;

    private Long userId;

    private String username;

    private String avatarUrl;


    private long commentCount;

    private long reactionCount;
}