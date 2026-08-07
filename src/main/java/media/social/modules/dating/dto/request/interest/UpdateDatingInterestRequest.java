package media.social.modules.dating.dto.request.interest;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateDatingInterestRequest {

    @NotEmpty(message = "Interest ids cannot be empty")
    private List<Long> interestIds;
}