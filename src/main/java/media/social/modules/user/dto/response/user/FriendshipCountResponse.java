package media.social.modules.user.dto.response.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class FriendshipCountResponse {
    private Long totalFriends;
}
