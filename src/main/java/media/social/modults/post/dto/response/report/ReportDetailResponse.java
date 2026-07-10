package media.social.modults.post.dto.response.report;

import lombok.Builder;
import lombok.Data;
import media.social.modults.post.enums.ReportStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportDetailResponse {

    private Long postId;

    private Long reporterId;

    private String reporterUsername;

    private String reporterAvatar;

    private String reason;

    private ReportStatus status;

    private Long reviewedBy;

    private String reviewedByUsername;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;

}