package media.social.modules.post.repository;

import media.social.modules.post.dto.projection.ReportDetailProjection;
import media.social.modules.post.dto.response.report.ReportDetailResponse;
import media.social.modules.post.entity.Report;
import media.social.modules.post.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndPost_Id(
            Long reporterId,
            Long postId
    );

    Page<Report> findByReporter_Id(
            Long reporterId,
            Pageable pageable
    );

    Page<Report> findByStatus(
            ReportStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT
            r.id AS reportId,
            p.id AS postId,
            reporter.id AS reporterId,
            reporter.username AS reporterUsername,
            profile.avatarUrl AS reporterAvatar,
            r.reason AS reason,
            r.status AS status,
            reviewer.id AS reviewedBy,
            reviewer.username AS reviewedByUsername,
            r.updatedAt AS updatedAt,
            r.createdAt AS createdAt
        FROM Report r
        JOIN r.post p
        JOIN r.reporter reporter
        LEFT JOIN reporter.profile profile
        LEFT JOIN r.reviewedBy reviewer
        ORDER BY r.createdAt DESC
    """)
    Page<ReportDetailProjection> getAll(Pageable pageable);

    @Query("""
        SELECT
            r.id AS reportId,
            p.id AS postId,
            reporter.id AS reporterId,
            reporter.username AS reporterUsername,
            profile.avatarUrl AS reporterAvatar,
            r.reason AS reason,
            r.status AS status,
            reviewer.id AS reviewedBy,
            reviewer.username AS reviewedByUsername,
            r.updatedAt AS updatedAt,
            r.createdAt AS createdAt
        FROM Report r
        JOIN r.post p
        JOIN r.reporter reporter
        LEFT JOIN reporter.profile profile
        LEFT JOIN r.reviewedBy reviewer
        WHERE r.status = :status
        ORDER BY r.createdAt DESC
    """)
    Page<ReportDetailProjection> getByStatus(
            @Param("status") ReportStatus status,
            Pageable pageable
    );

}