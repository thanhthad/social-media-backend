package media.social.modules.dating.repository;

import media.social.modules.dating.dto.response.report.DatingReportDetailResponse;
import media.social.modules.dating.entity.DatingReport;
import media.social.modules.dating.enums.DatingReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatingReportRepository
        extends JpaRepository<DatingReport, Long> {

    boolean existsByReporter_IdAndReportedUser_Id(
            Long reporterId,
            Long reportedUserId
    );

    @Query("""
        SELECT new media.social.modules.dating.dto.response.report.DatingReportDetailResponse(
            r.id,
            reporter.id,
            reporter.username,
            reporter.profile.avatarUrl,
            reportedUser.id,
            reportedUser.username,
            reportedUser.profile.avatarUrl,
            r.reason,
            r.status,
            reviewedBy.id,
            reviewedBy.username,
            r.reviewedAt,
            r.createdAt
        )
        FROM DatingReport r
        JOIN r.reporter reporter
        JOIN r.reportedUser reportedUser
        LEFT JOIN r.reviewedBy reviewedBy
        """)
    Page<DatingReportDetailResponse> getAll(Pageable pageable);

    @Query("""
        SELECT new media.social.modules.dating.dto.response.report.DatingReportDetailResponse(
            r.id,
            reporter.id,
            reporter.username,
            reporter.profile.avatarUrl,
            reportedUser.id,
            reportedUser.username,
            reportedUser.profile.avatarUrl,
            r.reason,
            r.status,
            reviewedBy.id,
            reviewedBy.username,
            r.reviewedAt,
            r.createdAt
        )
        FROM DatingReport r
        JOIN r.reporter reporter
        JOIN r.reportedUser reportedUser
        LEFT JOIN r.reviewedBy reviewedBy
        WHERE r.status = :status
        """)
    Page<DatingReportDetailResponse> getByStatus(
            @Param("status") DatingReportStatus status,
            Pageable pageable
    );
}