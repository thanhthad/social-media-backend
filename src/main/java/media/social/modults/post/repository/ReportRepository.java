package media.social.modults.post.repository;

import media.social.modults.post.dto.response.report.ReportDetailResponse;
import media.social.modults.post.entity.Report;
import media.social.modults.post.enums.ReportStatus;
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
        SELECT new media.social.modults.post.dto.response.report.ReportDetailResponse(
            p.id,
            reporter.id,
            reporter.username,
            profile.avatarUrl,
            r.reason,
            r.status,
            reviewer.id,
            reviewer.username,
            r.updatedAt,
            r.createdAt
        )
        FROM Report r
        JOIN r.post p
        JOIN r.reporter reporter
        LEFT JOIN reporter.profile profile
        LEFT JOIN r.reviewedBy reviewer
        ORDER BY r.createdAt DESC
    """)
    Page<ReportDetailResponse> getAll(Pageable pageable);

    @Query("""
        SELECT new media.social.modults.post.dto.response.report.ReportDetailResponse(
            p.id,
            reporter.id,
            reporter.username,
            profile.avatarUrl,
            r.reason,
            r.status,
            reviewer.id,
            reviewer.username,
            r.updatedAt,
            r.createdAt
        )
        FROM Report r
        JOIN r.post p
        JOIN r.reporter reporter
        LEFT JOIN reporter.profile profile
        LEFT JOIN r.reviewedBy reviewer
        WHERE r.status = :status
        ORDER BY r.createdAt DESC
    """)
    Page<ReportDetailResponse> getByStatus(
            @Param("status") ReportStatus status,
            Pageable pageable
    );

}