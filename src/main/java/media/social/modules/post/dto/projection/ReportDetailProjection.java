package media.social.modules.post.dto.projection;

import media.social.modules.post.enums.ReportStatus;

import java.time.LocalDateTime;

public interface ReportDetailProjection {

    Long getReportId();

    Long getPostId();

    Long getReporterId();

    String getReporterUsername();

    String getReporterAvatar();

    String getReason();

    ReportStatus getStatus();

    Long getReviewedBy();

    String getReviewedByUsername();

    LocalDateTime getUpdatedAt();

    LocalDateTime getCreatedAt();
}