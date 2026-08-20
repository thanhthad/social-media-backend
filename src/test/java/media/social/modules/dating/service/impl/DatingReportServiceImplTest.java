package media.social.modules.dating.service.impl;

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
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingReportServiceImplTest {

    @Mock
    private DatingReportRepository datingReportRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @InjectMocks
    private DatingReportServiceImpl datingReportService;

    // ---------------------------------------------------------------------------
    // create()
    // ---------------------------------------------------------------------------

    @Test
    void create_success_createsAndSavesReport() {
        Long currentUserId = 1L;
        Long reportedUserId = 2L;

        CreateDatingReportRequest request = new CreateDatingReportRequest();
        request.setReportedUserId(reportedUserId);
        request.setReason("Inappropriate behavior");

        User reporter = new User();
        reporter.setId(currentUserId);

        User reportedUser = new User();
        reportedUser.setId(reportedUserId);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(datingReportRepository.existsByReporter_IdAndReportedUser_Id(currentUserId, reportedUserId))
                    .thenReturn(false);
            when(userServiceDomain.getByUserId(currentUserId)).thenReturn(reporter);
            when(userServiceDomain.getByUserId(reportedUserId)).thenReturn(reportedUser);

            datingReportService.create(request);

            ArgumentCaptor<DatingReport> captor = ArgumentCaptor.forClass(DatingReport.class);
            verify(datingReportRepository).save(captor.capture());

            DatingReport saved = captor.getValue();
            assertThat(saved.getReporter()).isEqualTo(reporter);
            assertThat(saved.getReportedUser()).isEqualTo(reportedUser);
            assertThat(saved.getReason()).isEqualTo("Inappropriate behavior");
        }
    }

    @Test
    void create_alreadyReported_throwsDatingReportAlreadyExistsException() {
        Long currentUserId = 1L;
        Long reportedUserId = 2L;

        CreateDatingReportRequest request = new CreateDatingReportRequest();
        request.setReportedUserId(reportedUserId);
        request.setReason("Spam");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(datingReportRepository.existsByReporter_IdAndReportedUser_Id(currentUserId, reportedUserId))
                    .thenReturn(true);

            assertThrows(DatingReportAlreadyExistsException.class,
                    () -> datingReportService.create(request));

            verify(datingReportRepository, never()).save(any());
            verify(userServiceDomain, never()).getByUserId(any());
        }
    }

    @Test
    void create_ownProfile_throwsCannotReportOwnProfileException() {
        Long currentUserId = 1L;
        Long reportedUserId = 1L; // same as currentUserId → self-report

        CreateDatingReportRequest request = new CreateDatingReportRequest();
        request.setReportedUserId(reportedUserId);
        request.setReason("Testing self-report");

        User reporter = new User();
        reporter.setId(currentUserId);

        User reportedUser = new User();
        reportedUser.setId(reportedUserId);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(datingReportRepository.existsByReporter_IdAndReportedUser_Id(currentUserId, reportedUserId))
                    .thenReturn(false);
            when(userServiceDomain.getByUserId(currentUserId)).thenReturn(reporter);
            when(userServiceDomain.getByUserId(reportedUserId)).thenReturn(reportedUser);

            assertThrows(CannotReportOwnProfileException.class,
                    () -> datingReportService.create(request));

            verify(datingReportRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------------
    // review()
    // ---------------------------------------------------------------------------

    @Test
    void review_success_approveReport() {
        Long reviewerUserId = 10L;
        Long reportId = 100L;

        User reviewer = new User();
        reviewer.setId(reviewerUserId);

        DatingReport report = DatingReport.builder()
                .reporter(new User())
                .reportedUser(new User())
                .reason("Abusive content")
                .build();
        report.setStatus(DatingReportStatus.PENDING);

        UpdateDatingReportRequest request = new UpdateDatingReportRequest();
        request.setReportStatus(DatingReportStatus.APPROVED);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(reviewerUserId);

            when(userServiceDomain.getByUserId(reviewerUserId)).thenReturn(reviewer);
            when(datingReportRepository.findById(reportId)).thenReturn(Optional.of(report));

            datingReportService.review(reportId, request);

            ArgumentCaptor<DatingReport> captor = ArgumentCaptor.forClass(DatingReport.class);
            verify(datingReportRepository).save(captor.capture());

            DatingReport saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(DatingReportStatus.APPROVED);
            assertThat(saved.getReviewedBy()).isEqualTo(reviewer);
            assertThat(saved.getReviewedAt()).isNotNull();
        }
    }

    @Test
    void review_success_rejectReport() {
        Long reviewerUserId = 10L;
        Long reportId = 101L;

        User reviewer = new User();
        reviewer.setId(reviewerUserId);

        DatingReport report = DatingReport.builder()
                .reporter(new User())
                .reportedUser(new User())
                .reason("Fake profile")
                .build();
        report.setStatus(DatingReportStatus.PENDING);

        UpdateDatingReportRequest request = new UpdateDatingReportRequest();
        request.setReportStatus(DatingReportStatus.REJECTED);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(reviewerUserId);

            when(userServiceDomain.getByUserId(reviewerUserId)).thenReturn(reviewer);
            when(datingReportRepository.findById(reportId)).thenReturn(Optional.of(report));

            datingReportService.review(reportId, request);

            ArgumentCaptor<DatingReport> captor = ArgumentCaptor.forClass(DatingReport.class);
            verify(datingReportRepository).save(captor.capture());

            DatingReport saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(DatingReportStatus.REJECTED);
            assertThat(saved.getReviewedBy()).isEqualTo(reviewer);
            assertThat(saved.getReviewedAt()).isNotNull();
        }
    }

    @Test
    void review_reportNotFound_throwsDatingReportNotFoundException() {
        Long reviewerUserId = 10L;
        Long reportId = 999L;

        User reviewer = new User();
        reviewer.setId(reviewerUserId);

        UpdateDatingReportRequest request = new UpdateDatingReportRequest();
        request.setReportStatus(DatingReportStatus.APPROVED);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(reviewerUserId);

            when(userServiceDomain.getByUserId(reviewerUserId)).thenReturn(reviewer);
            when(datingReportRepository.findById(reportId)).thenReturn(Optional.empty());

            assertThrows(DatingReportNotFoundException.class,
                    () -> datingReportService.review(reportId, request));

            verify(datingReportRepository, never()).save(any());
        }
    }

    @Test
    void review_withPendingStatus_throwsDatingReportAlreadyExistsException() {
        Long reviewerUserId = 10L;
        Long reportId = 100L;

        User reviewer = new User();
        reviewer.setId(reviewerUserId);

        DatingReport report = DatingReport.builder()
                .reporter(new User())
                .reportedUser(new User())
                .reason("Harassment")
                .build();
        report.setStatus(DatingReportStatus.PENDING);

        UpdateDatingReportRequest request = new UpdateDatingReportRequest();
        request.setReportStatus(DatingReportStatus.PENDING); // not allowed

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(reviewerUserId);

            when(userServiceDomain.getByUserId(reviewerUserId)).thenReturn(reviewer);
            when(datingReportRepository.findById(reportId)).thenReturn(Optional.of(report));

            assertThrows(DatingReportAlreadyExistsException.class,
                    () -> datingReportService.review(reportId, request));

            verify(datingReportRepository, never()).save(any());
        }
    }

    @Test
    void review_alreadyReviewed_throwsDatingReportAlreadyReviewedException() {
        Long reviewerUserId = 10L;
        Long reportId = 100L;

        User reviewer = new User();
        reviewer.setId(reviewerUserId);

        DatingReport report = DatingReport.builder()
                .reporter(new User())
                .reportedUser(new User())
                .reason("Nudity")
                .build();
        report.setStatus(DatingReportStatus.APPROVED); // already reviewed

        UpdateDatingReportRequest request = new UpdateDatingReportRequest();
        request.setReportStatus(DatingReportStatus.REJECTED);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(reviewerUserId);

            when(userServiceDomain.getByUserId(reviewerUserId)).thenReturn(reviewer);
            when(datingReportRepository.findById(reportId)).thenReturn(Optional.of(report));

            assertThrows(DatingReportAlreadyReviewedException.class,
                    () -> datingReportService.review(reportId, request));

            verify(datingReportRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------------
    // getAll()
    // ---------------------------------------------------------------------------

    @Test
    void getAll_success_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DatingReportDetailResponse> expectedPage =
                new PageImpl<>(List.of(mock(DatingReportDetailResponse.class)));

        when(datingReportRepository.getAll(pageable)).thenReturn(expectedPage);

        Page<DatingReportDetailResponse> result = datingReportService.getAll(pageable);

        assertThat(result).isEqualTo(expectedPage);
        verify(datingReportRepository).getAll(pageable);
    }

    // ---------------------------------------------------------------------------
    // getByStatus()
    // ---------------------------------------------------------------------------

    @Test
    void getByStatus_success_returnsPageFilteredByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        DatingReportStatus status = DatingReportStatus.PENDING;
        Page<DatingReportDetailResponse> expectedPage =
                new PageImpl<>(List.of(mock(DatingReportDetailResponse.class)));

        when(datingReportRepository.getByStatus(status, pageable)).thenReturn(expectedPage);

        Page<DatingReportDetailResponse> result = datingReportService.getByStatus(pageable, status);

        assertThat(result).isEqualTo(expectedPage);
        verify(datingReportRepository).getByStatus(status, pageable);
    }
}
