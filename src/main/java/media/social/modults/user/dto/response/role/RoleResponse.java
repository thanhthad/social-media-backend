package media.social.modults.user.dto.response.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RoleResponse {

    private String name;
    private String description;
}