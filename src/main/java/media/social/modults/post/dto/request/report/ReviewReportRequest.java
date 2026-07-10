package media.social.modults.post.dto.request.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import media.social.modults.post.enums.ReportStatus;

@Data
public class ReviewReportRequest {

    @NotNull
    private ReportStatus status;

}
