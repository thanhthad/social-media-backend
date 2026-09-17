package media.social.modules.dating.dto.response.match;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatingMatchResponse {
    private Long matchId;
    private Long matchedUserId;
    private String username;
    private String name;
    private String avatarUrl;
    private String bio;
    private OffsetDateTime matchedAt;
}
