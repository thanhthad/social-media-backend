package media.social.modules.post.dto.request.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import media.social.modules.post.enums.ReportStatus;

@Data
public class UpdateReportRequest {

    @NotNull
    private ReportStatus reportStatus;

}