package media.social.modults.user.dto.response.pub;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSearchResponse {

    private Long id;
    private String username;
    private String avatarUrl;
    private String fullName;
}