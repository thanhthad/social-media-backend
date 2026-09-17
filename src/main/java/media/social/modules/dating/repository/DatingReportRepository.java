package media.social.modules.dating.repository;

import media.social.modules.dating.dto.projection.DatingReportDetailProjection;
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
    SELECT
        r.id AS reportId,
        reporter.id AS reporterId,
        reporter.username AS reporterUsername,
        reporter.profile.avatarUrl AS reporterAvatar,
        reportedUser.id AS reportedUserId,
        reportedUser.username AS reportedUsername,
        reportedUser.profile.avatarUrl AS reportedAvatar,
        r.reason AS reason,
        r.status AS status,
        reviewedBy.id AS reviewedBy,
        reviewedBy.username AS reviewedByUsername,
        r.reviewedAt AS reviewedAt,
        r.createdAt AS createdAt
    FROM DatingReport r
    JOIN r.reporter reporter
    JOIN r.reportedUser reportedUser
    LEFT JOIN r.reviewedBy reviewedBy
""")
    Page<DatingReportDetailProjection> getAll(Pageable pageable);

    @Query("""
    SELECT
        r.id AS reportId,
        reporter.id AS reporterId,
        reporter.username AS reporterUsername,
        reporter.profile.avatarUrl AS reporterAvatar,
        reportedUser.id AS reportedUserId,
        reportedUser.username AS reportedUsername,
        reportedUser.profile.avatarUrl AS reportedAvatar,
        r.reason AS reason,
        r.status AS status,
        reviewedBy.id AS reviewedBy,
        reviewedBy.username AS reviewedByUsername,
        r.reviewedAt AS reviewedAt,
        r.createdAt AS createdAt
    FROM DatingReport r
    JOIN r.reporter reporter
    JOIN r.reportedUser reportedUser
    LEFT JOIN r.reviewedBy reviewedBy
    WHERE r.status = :status
""")
    Page<DatingReportDetailProjection> getByStatus(
            @Param("status") DatingReportStatus status,
            Pageable pageable
    );
}