package media.social.modules.dating.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDatingReportRequest {

    @NotNull(message = "Reported user ID is required")
    private Long reportedUserId;

    @NotBlank(message = "Report reason is required")
    @Size(
            min = 10,
            max = 1000,
            message = "Report reason must be between 10 and 1000 characters"
    )
    private String reason;
}