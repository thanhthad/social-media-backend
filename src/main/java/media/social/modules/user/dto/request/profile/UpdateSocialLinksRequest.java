package media.social.modules.user.dto.request.profile;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UpdateSocialLinksRequest {

    @Size(
            max = 5,
            message = "Maximum 10 social links allowed"
    )
    private Map<String,String> socialLinks;

}