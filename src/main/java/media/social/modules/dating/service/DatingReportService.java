package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.report.CreateDatingReportRequest;
import media.social.modules.dating.dto.request.report.UpdateDatingReportRequest;
import media.social.modules.dating.dto.response.report.DatingReportDetailResponse;
import media.social.modules.dating.enums.DatingReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatingReportService {

    void create(CreateDatingReportRequest request);

    void review(
            Long reportId,
            UpdateDatingReportRequest request
    );

    Page<DatingReportDetailResponse> getAll(Pageable pageable);

    Page<DatingReportDetailResponse> getByStatus(
            Pageable pageable,
            DatingReportStatus reportStatus
    );
}