package media.social.modules.dating.dto.request.report;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.dating.enums.DatingReportStatus;

@Getter
@Setter
public class UpdateDatingReportRequest {

    @NotNull(message = "Report status is required")
    private DatingReportStatus reportStatus;
}