package media.social.modules.user.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private Long id;

    private String username;

    private String email;

    private ProfileResponse profile;

    private Long totalFollower;

    private Long totalFollowing;

}