package media.social.modults.post.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long commentId;

    private Long userId;

    private String username;

    private String avatarUrl;

    private Long parentId;

    private String content;

    private LocalDateTime createdAt;

//    private Long totalLikes;

    private Long totalReplies;

}