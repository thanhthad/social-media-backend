package media.social.modules.user.dto.response.block;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ListUserBlockedResponse {
    private Long id;

    private String username;

    private String fullName;

    private String avatarUrl;
}