package media.social.modults.user.dto.response.user;

import lombok.Builder;
import lombok.Getter;
import media.social.modults.user.Enum.Status;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;

    private String username;

    private String email;

    private Status status;

    private ProfileResponse profile;
}