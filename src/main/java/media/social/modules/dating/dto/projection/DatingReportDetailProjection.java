package media.social.modules.dating.dto.projection;

import media.social.modules.dating.enums.DatingReportStatus;

import java.time.OffsetDateTime;

public interface DatingReportDetailProjection {

    Long getReportId();

    Long getReporterId();

    String getReporterUsername();

    String getReporterAvatar();

    Long getReportedUserId();

    String getReportedUsername();

    String getReportedAvatar();

    String getReason();

    DatingReportStatus getStatus();

    Long getReviewedBy();

    String getReviewedByUsername();

    OffsetDateTime getReviewedAt();

    OffsetDateTime getCreatedAt();
}