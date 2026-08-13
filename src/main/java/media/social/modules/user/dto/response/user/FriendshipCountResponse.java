package media.social.modules.user.dto.response.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipCountResponse {

    private Long totalFriends;
}