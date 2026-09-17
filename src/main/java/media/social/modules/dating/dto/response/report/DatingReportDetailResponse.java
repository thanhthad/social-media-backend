package media.social.modules.dating.dto.response.report;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.dating.enums.DatingReportStatus;

import java.time.OffsetDateTime;

@Getter
@Builder
public class DatingReportDetailResponse {

    private Long reportId;

    private Long reporterId;

    private String reporterUsername;

    private String reporterAvatar;

    private Long reportedUserId;

    private String reportedUsername;

    private String reportedAvatar;

    private String reason;

    private DatingReportStatus status;

    private Long reviewedBy;

    private String reviewedByUsername;

    private OffsetDateTime reviewedAt;

    private OffsetDateTime createdAt;
}