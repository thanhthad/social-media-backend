package media.social.modults.others.dto.response;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PostResponse {

    private Long id;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String avatarUrl;

}
