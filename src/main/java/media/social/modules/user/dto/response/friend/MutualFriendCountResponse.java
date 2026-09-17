package media.social.modules.user.dto.response.friend;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MutualFriendCountResponse {

    private Long totalMutualCount;
}