package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.report.CreateDatingReportRequest;
import media.social.modules.dating.dto.request.report.UpdateDatingReportRequest;
import media.social.modules.dating.dto.response.report.DatingReportDetailResponse;
import media.social.modules.dating.entity.DatingReport;
import media.social.modules.dating.enums.DatingReportStatus;
import media.social.modules.dating.exception.report.CannotReportOwnProfileException;
import media.social.modules.dating.exception.report.DatingReportAlreadyExistsException;
import media.social.modules.dating.exception.report.DatingReportAlreadyReviewedException;
import media.social.modules.dating.exception.report.DatingReportNotFoundException;
import media.social.modules.dating.repository.DatingReportRepository;
import media.social.modules.dating.service.DatingReportService;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DatingReportServiceImpl
        implements DatingReportService {

    private final DatingReportRepository datingReportRepository;
    private final UserServiceDomain userServiceDomain;

    @Override
    @Transactional
    public void create(
            CreateDatingReportRequest request
    ) {

        Long userId = UserContextHolder.getUserId();

        if (datingReportRepository.existsByReporter_IdAndReportedUser_Id(
                userId,
                request.getReportedUserId()
        )) {
            throw new DatingReportAlreadyExistsException(
                    "You already sent a report for this user."
            );
        }

        User reporter = userServiceDomain.getByUserId(userId);

        User reportedUser = userServiceDomain.getByUserId(
                request.getReportedUserId()
        );

        if (reportedUser.getId().equals(userId)) {
            throw new CannotReportOwnProfileException(
                    "You cannot report your own dating profile."
            );
        }

        DatingReport report = DatingReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(request.getReason())
                .build();

        datingReportRepository.save(report);
    }

    @Override
    @Transactional
    public void review(
            Long reportId,
            UpdateDatingReportRequest request
    ) {

        User reviewer = userServiceDomain.getByUserId(
                UserContextHolder.getUserId()
        );

        DatingReport report = datingReportRepository.findById(reportId)
                .orElseThrow(() ->
                        new DatingReportNotFoundException(
                                "Dating report not found"
                        )
                );

        if (request.getReportStatus()
                .equals(DatingReportStatus.PENDING)) {

            throw new DatingReportAlreadyExistsException(
                    "Report cannot be reviewed with PENDING status."
            );
        }

        if (report.getStatus() != DatingReportStatus.PENDING) {

            throw new DatingReportAlreadyReviewedException(
                    "Report has already been reviewed."
            );
        }

        report.setStatus(request.getReportStatus());
        report.setReviewedBy(reviewer);
        report.setReviewedAt(OffsetDateTime.now());

        datingReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DatingReportDetailResponse> getAll(
            Pageable pageable
    ) {

        return datingReportRepository.getAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DatingReportDetailResponse> getByStatus(
            Pageable pageable,
            DatingReportStatus reportStatus
    ) {

        return datingReportRepository.getByStatus(
                reportStatus,
                pageable
        );
    }
}