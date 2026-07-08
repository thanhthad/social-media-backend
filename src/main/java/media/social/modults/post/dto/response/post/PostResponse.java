package media.social.modults.post.dto.response.post;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostResponse {

    private Long id;
    private String content;
    private List<PostMediaResponse> postMediaResponses = new ArrayList<>();

    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String avatarUrl;
}