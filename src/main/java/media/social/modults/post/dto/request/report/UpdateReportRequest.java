package media.social.modults.post.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import media.social.modults.post.enums.ReportStatus;

@Data
public class UpdateReportRequest {

    @NotBlank
    private ReportStatus reportStatus;

}