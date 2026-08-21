package media.social.modules.post.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.dto.projection.ReportDetailProjection;
import media.social.modules.post.dto.request.report.CreateReportRequest;
import media.social.modules.post.dto.request.report.UpdateReportRequest;
import media.social.modules.post.dto.response.report.ReportDetailResponse;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.Report;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.exception.post.PostPrivateException;
import media.social.modules.post.exception.report.CannotReportOwnPostException;
import media.social.modules.post.exception.report.ReportAlreadyExistsException;
import media.social.modules.post.exception.report.ReportAlreadyReviewedException;
import media.social.modules.post.exception.report.ReportNotFoundException;
import media.social.modules.post.exception.saved_post.PostFriendsOnlyException;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.repository.ReportRepository;
import media.social.modules.post.service.impl.ReportServiceImpl;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.FriendShipDomain;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Mock
    private FriendShipDomain friendShipDomain;

    @Test
    void create_success_publicPost() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);
        request.setReason("Spam content");

        User reporter = new User();
        reporter.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.PUBLIC)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(reporter);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            reportService.create(request);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            Report saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(reporter, saved.getReporter());
            assertEquals(post, saved.getPost());
            assertEquals("Spam content", saved.getReason());
        }
    }

    @Test
    void create_success_friendPost_areFriends() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);
        request.setReason("Inappropriate");

        User reporter = new User();
        reporter.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.FRIEND)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(reporter);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(friendShipDomain.areFriends(userId, ownerId)).thenReturn(true);

            reportService.create(request);

            verify(reportRepository).save(any(Report.class));
        }
    }

    @Test
    void create_alreadyReported_throwsReportAlreadyExistsException() {
        Long userId = 1L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(true);

            assertThrows(ReportAlreadyExistsException.class, () -> reportService.create(request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void create_postNotFound_throwsPostNotFoundException() {
        Long userId = 1L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);

        User reporter = new User();
        reporter.setId(userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(reporter);
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> reportService.create(request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void create_ownPost_throwsCannotReportOwnPostException() {
        Long userId = 1L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);

        User reporter = new User();
        reporter.setId(userId);

        Post post = Post.builder()
                .user(reporter)
                .visibility(Visibility.PUBLIC)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(reporter);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(CannotReportOwnPostException.class, () -> reportService.create(request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void create_privatePost_throwsPostPrivateException() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);

        User reporter = new User();
        reporter.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.PRIVATE)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(reporter);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PostPrivateException.class, () -> reportService.create(request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void create_friendPost_notFriends_throwsPostFriendsOnlyException() {
        Long userId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        CreateReportRequest request = new CreateReportRequest();
        request.setPostId(postId);

        User reporter = new User();
        reporter.setId(userId);

        User owner = new User();
        owner.setId(ownerId);

        Post post = Post.builder()
                .user(owner)
                .visibility(Visibility.FRIEND)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(reportRepository.existsByReporter_IdAndPost_Id(userId, postId)).thenReturn(false);
            when(userServiceDomain.getByUserId(userId)).thenReturn(reporter);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(friendShipDomain.areFriends(userId, ownerId)).thenReturn(false);

            assertThrows(PostFriendsOnlyException.class, () -> reportService.create(request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void review_success() {
        Long reportId = 1L;
        Long adminId = 10L;

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportStatus(ReportStatus.APPROVED);

        User reviewer = new User();
        reviewer.setId(adminId);

        Report report = Report.builder()
                .status(ReportStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(adminId)).thenReturn(reviewer);
            when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

            reportService.review(reportId, request);

            assertEquals(ReportStatus.APPROVED, report.getStatus());
            assertEquals(reviewer, report.getReviewedBy());
            verify(reportRepository).save(report);
        }
    }

    @Test
    void review_reportNotFound_throwsReportNotFoundException() {
        Long reportId = 1L;
        Long adminId = 10L;

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportStatus(ReportStatus.APPROVED);

        User reviewer = new User();
        reviewer.setId(adminId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(adminId)).thenReturn(reviewer);
            when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

            assertThrows(ReportNotFoundException.class, () -> reportService.review(reportId, request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void review_pendingStatusRequested_throwsReportAlreadyExistsException() {
        Long reportId = 1L;
        Long adminId = 10L;

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportStatus(ReportStatus.PENDING);

        User reviewer = new User();
        reviewer.setId(adminId);

        Report report = Report.builder()
                .status(ReportStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(adminId)).thenReturn(reviewer);
            when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

            assertThrows(ReportAlreadyExistsException.class, () -> reportService.review(reportId, request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void review_alreadyReviewed_throwsReportAlreadyReviewedException() {
        Long reportId = 1L;
        Long adminId = 10L;

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportStatus(ReportStatus.APPROVED);

        User reviewer = new User();
        reviewer.setId(adminId);

        Report report = Report.builder()
                .status(ReportStatus.REJECTED)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(adminId)).thenReturn(reviewer);
            when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

            assertThrows(ReportAlreadyReviewedException.class, () -> reportService.review(reportId, request));
            verify(reportRepository, never()).save(any());
        }
    }

    @Test
    void getAll_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ReportDetailProjection projection = mock(ReportDetailProjection.class);
        when(projection.getReportId()).thenReturn(1L);
        Page<ReportDetailProjection> expectedPage = new PageImpl<>(List.of(projection));

        when(reportRepository.getAll(pageable)).thenReturn(expectedPage);

        Page<ReportDetailResponse> result = reportService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getReportId());
        verify(reportRepository).getAll(pageable);
    }

    @Test
    void getByStatus_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ReportStatus status = ReportStatus.APPROVED;
        ReportDetailProjection projection = mock(ReportDetailProjection.class);
        when(projection.getReportId()).thenReturn(1L);
        Page<ReportDetailProjection> expectedPage = new PageImpl<>(List.of(projection));

        when(reportRepository.getByStatus(status, pageable)).thenReturn(expectedPage);

        Page<ReportDetailResponse> result = reportService.getByStatus(pageable, status);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getReportId());
        verify(reportRepository).getByStatus(status, pageable);
    }

    @Test
    void review_rejectedStatus_success() {
        Long reportId = 1L;
        Long adminId = 10L;

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportStatus(ReportStatus.REJECTED);

        User reviewer = new User();
        reviewer.setId(adminId);

        Report report = Report.builder()
                .status(ReportStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(adminId);

            when(userServiceDomain.getByUserId(adminId)).thenReturn(reviewer);
            when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

            reportService.review(reportId, request);

            assertEquals(ReportStatus.REJECTED, report.getStatus());
            assertEquals(reviewer, report.getReviewedBy());
            verify(reportRepository).save(report);
        }
    }
}
