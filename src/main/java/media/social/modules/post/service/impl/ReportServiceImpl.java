package media.social.modules.post.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.post.dto.request.report.CreateReportRequest;
import media.social.modules.post.dto.request.report.UpdateReportRequest;
import media.social.modules.post.dto.response.report.ReportDetailResponse;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.Report;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.exception.post.PostFollowersOnlyException;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.exception.post.PostPrivateException;
import media.social.modules.post.exception.report.CannotReportOwnPostException;
import media.social.modules.post.exception.report.ReportAlreadyExistsException;
import media.social.modules.post.exception.report.ReportAlreadyReviewedException;
import media.social.modules.post.exception.report.ReportNotFoundException;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.repository.ReportRepository;
import media.social.modules.post.service.ReportService;
import media.social.modules.user.entity.User;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.FollowService;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserServiceDomain userServiceDomain;
    private final FollowService followService;

    @Override
    @Transactional
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

        if (post.getUser().getId().equals(userId)) {
            throw new CannotReportOwnPostException("You cannot report your own post.");
        }

        switch (post.getVisibility()) {
            case PRIVATE ->
                    throw new PostPrivateException("You cannot report this private post.");

            case FOLLOWERS -> {
                if (!followService.isFollowing(post.getUser().getId())) {
                    throw new PostFollowersOnlyException(
                            "You must follow this user to report this post.");
                }
            }
            case PUBLIC -> {
            }
        }

        Report report = Report.builder()
                .reporter(user)
                .post(post)
                .reason(request.getReason())
                .build();

        reportRepository.save(report);
    }

    @Transactional
    @Override
    public void review(
            Long reportId,
            UpdateReportRequest request
    ) {
        User user = userServiceDomain.getByUserId(UserContextHolder.getUserId());
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found"));
        if(request.getReportStatus().equals(ReportStatus.PENDING)){
            throw new ReportAlreadyExistsException("Report already PENDING status");
        }
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