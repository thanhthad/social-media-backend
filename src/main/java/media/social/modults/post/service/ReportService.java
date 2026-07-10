package media.social.modults.post.service;

import media.social.modults.post.dto.request.report.CreateReportRequest;
import media.social.modults.post.dto.request.report.UpdateReportRequest;
import media.social.modults.post.dto.response.report.ReportDetailResponse;
import media.social.modults.post.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    void create(
            CreateReportRequest request
    );

    void review(
            Long reportId,
            UpdateReportRequest request
    );

    Page<ReportDetailResponse> getAll(
            Pageable pageable
    );

    Page<ReportDetailResponse> getByStatus(
            Pageable pageable,
            ReportStatus reportStatus
    );

}