package media.social.modults.post.dto.response.hashtag;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingHashtagResponse {

    private Long id;

    private String name;

    private Long totalPosts;

}