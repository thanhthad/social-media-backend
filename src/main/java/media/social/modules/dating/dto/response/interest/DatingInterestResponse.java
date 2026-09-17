package media.social.modules.dating.dto.response.interest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatingInterestResponse {

    private Long id;

    private String name;
}