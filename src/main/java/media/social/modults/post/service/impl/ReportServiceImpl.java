package media.social.modults.post.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.post.dto.request.report.CreateReportRequest;
import media.social.modults.post.dto.request.report.UpdateReportRequest;
import media.social.modults.post.dto.response.report.ReportDetailResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.Report;
import media.social.modults.post.enums.ReportStatus;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.exception.report.ReportAlreadyExistsException;
import media.social.modults.post.exception.report.ReportAlreadyReviewedException;
import media.social.modults.post.exception.report.ReportNotFoundException;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.post.repository.ReportRepository;
import media.social.modults.post.service.ReportService;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserServiceDomain userServiceDomain;

    @Override
    public void create(
            CreateReportRequest request
    ) {
        Long userId = UserContextHolder.getUserId();
        if (reportRepository.existsByReporter_IdAndPost_Id(
                userId,
                request.getPostId()
        )) {
            throw new ReportAlreadyExistsException("You already send report for this post");
        }
        User user = userServiceDomain.getByUserId(userId);

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        Report report = Report.builder()
                .reporter(user)
                .post(post)
                .reason(request.getReason())
                .build();

        reportRepository.save(report);
    }

    @Override
    public void review(
            Long reportId,
            UpdateReportRequest request
    ) {
        User user = userServiceDomain.getByUserId(UserContextHolder.getUserId());
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found"));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ReportAlreadyReviewedException("Report has already been reviewed.");
        }
        report.setStatus(request.getReportStatus());
        report.setReviewedBy(user);

        reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportDetailResponse> getAll(Pageable pageable) {

        return reportRepository.getAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportDetailResponse> getByStatus(
            Pageable pageable,
            ReportStatus reportStatus
    ) {

        return reportRepository.getByStatus(
                reportStatus,
                pageable
        );
    }

}