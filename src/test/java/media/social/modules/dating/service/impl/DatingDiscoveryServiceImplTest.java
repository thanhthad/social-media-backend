package media.social.modules.dating.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.response.discovery.DatingDiscoveryResponse;
import media.social.modules.dating.dto.response.projection.DatingDiscoveryProjection;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.exception.profile.CoordinatesNotFoundException;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.repository.DatingProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingDiscoveryServiceImplTest {

    @Mock
    private DatingProfileRepository profileRepository;

    @InjectMocks
    private DatingDiscoveryServiceImpl datingDiscoveryService;

    private static final Long USER_ID = 1L;

    @Test
    void findDiscovery_profileNotFound_throwsDatingProfileNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(DatingProfileNotFoundException.class,
                    () -> datingDiscoveryService.findDiscovery(PageRequest.of(0, 10)));

            verify(profileRepository).findByUserId(USER_ID);
            verify(profileRepository, never()).findDiscoveryCandidates(any(), any());
        }
    }

    @Test
    void findDiscovery_coordinatesNull_throwsCoordinatesNotFoundException() {
        DatingProfile profile = DatingProfile.builder()
                .id(10L)
                .latitude(null)
                .longitude(null)
                .birthday(LocalDate.of(1995, 1, 1))
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            assertThrows(CoordinatesNotFoundException.class,
                    () -> datingDiscoveryService.findDiscovery(PageRequest.of(0, 10)));

            verify(profileRepository).findByUserId(USER_ID);
            verify(profileRepository, never()).findDiscoveryCandidates(any(), any());
        }
    }

    @Test
    void findDiscovery_birthdayNull_throwsBadRequestException() {
        DatingProfile profile = DatingProfile.builder()
                .id(10L)
                .latitude(BigDecimal.valueOf(21.0285))
                .longitude(BigDecimal.valueOf(105.8542))
                .birthday(null)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            assertThrows(BadRequestException.class,
                    () -> datingDiscoveryService.findDiscovery(PageRequest.of(0, 10)));

            verify(profileRepository).findByUserId(USER_ID);
            verify(profileRepository, never()).findDiscoveryCandidates(any(), any());
        }
    }

    @Test
    void findDiscovery_success_calculatesCorrectScores() {
        LocalDate currentBirthday = LocalDate.now().minusYears(25); // Current user is exactly 25 years old
        DatingProfile profile = DatingProfile.builder()
                .id(10L)
                .latitude(BigDecimal.valueOf(21.0285))
                .longitude(BigDecimal.valueOf(105.8542))
                .birthday(currentBirthday)
                .build();

        DatingDiscoveryProjection candidate = mock(DatingDiscoveryProjection.class);
        when(candidate.getUserId()).thenReturn(2L);
        when(candidate.getDisplayName()).thenReturn("Alice");
        when(candidate.getAvatarUrl()).thenReturn("https://example.com/alice.png");
        when(candidate.getAge()).thenReturn(27); // target age is 27 (difference is 2 years)
        when(candidate.getCity()).thenReturn("Hanoi");
        when(candidate.getDistanceKm()).thenReturn(10.0); // distance is 10 km
        when(candidate.getCommonInterestCount()).thenReturn(2L); // 2 interests

        Pageable pageable = PageRequest.of(0, 10);
        Page<DatingDiscoveryProjection> projectionPage = new PageImpl<>(List.of(candidate));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.findDiscoveryCandidates(USER_ID, pageable)).thenReturn(projectionPage);

            Page<DatingDiscoveryResponse> result = datingDiscoveryService.findDiscovery(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            DatingDiscoveryResponse response = result.getContent().get(0);
            assertThat(response.getUserId()).isEqualTo(2L);
            assertThat(response.getDisplayName()).isEqualTo("Alice");
            assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/alice.png");
            assertThat(response.getAge()).isEqualTo(27);
            assertThat(response.getCity()).isEqualTo("Hanoi");
            assertThat(response.getDistanceKm()).isEqualTo(10.0);

            // Calculations:
            // interestScore: 2 interests -> 75.0 * 0.15 = 11.25
            // ageScore: abs(25 - 27) = 2 -> 100 - (2 * 10) = 80.0 * 0.20 = 16.0
            // distanceScore: 100 - (10.0 * 2) = 80.0 * 0.40 = 32.0
            // recencyScore: null -> 0.0
            // qualityScore: null -> 0.0
            // Compatibility Score = 11.25 + 16.0 + 32.0 = 59.25 -> rounded to 59
            assertThat(response.getCompatibilityScore()).isEqualTo(59);
        }
    }

    @Test
    void findDiscovery_compatibilityScore_interestScoreBounds() {
        LocalDate currentBirthday = LocalDate.now().minusYears(30);
        DatingProfile profile = DatingProfile.builder()
                .id(10L)
                .latitude(BigDecimal.valueOf(21.0285))
                .longitude(BigDecimal.valueOf(105.8542))
                .birthday(currentBirthday)
                .build();

        // Test with different interest counts: null/0, 1, 3
        DatingDiscoveryProjection candidate1 = mock(DatingDiscoveryProjection.class);
        when(candidate1.getUserId()).thenReturn(2L);
        when(candidate1.getAge()).thenReturn(30); // age diff = 0
        when(candidate1.getDistanceKm()).thenReturn(0.0); // distance = 0
        when(candidate1.getCommonInterestCount()).thenReturn(null); // interestScore = 0

        DatingDiscoveryProjection candidate2 = mock(DatingDiscoveryProjection.class);
        when(candidate2.getUserId()).thenReturn(3L);
        when(candidate2.getAge()).thenReturn(30); // age diff = 0
        when(candidate2.getDistanceKm()).thenReturn(0.0); // distance = 0
        when(candidate2.getCommonInterestCount()).thenReturn(1L); // interestScore = 50

        DatingDiscoveryProjection candidate3 = mock(DatingDiscoveryProjection.class);
        when(candidate3.getUserId()).thenReturn(4L);
        when(candidate3.getAge()).thenReturn(30); // age diff = 0
        when(candidate3.getDistanceKm()).thenReturn(0.0); // distance = 0
        when(candidate3.getCommonInterestCount()).thenReturn(3L); // interestScore = 100

        Pageable pageable = PageRequest.of(0, 10);
        Page<DatingDiscoveryProjection> projectionPage = new PageImpl<>(List.of(candidate1, candidate2, candidate3));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.findDiscoveryCandidates(USER_ID, pageable)).thenReturn(projectionPage);

            Page<DatingDiscoveryResponse> result = datingDiscoveryService.findDiscovery(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);

            // Candidate 1: interestScore = 0. ageScore = 100. distanceScore = 100.
            // score = 0 * 0.15 + 100 * 0.20 + 100 * 0.40 = 60
            assertThat(result.getContent().get(0).getCompatibilityScore()).isEqualTo(60);

            // Candidate 2: interestScore = 50. ageScore = 100. distanceScore = 100.
            // score = 50 * 0.15 + 100 * 0.20 + 100 * 0.40 = 7.5 + 20 + 40 = 67.5 -> rounded to 68
            assertThat(result.getContent().get(1).getCompatibilityScore()).isEqualTo(68);

            // Candidate 3: interestScore = 100. ageScore = 100. distanceScore = 100.
            // score = 100 * 0.15 + 100 * 0.20 + 100 * 0.40 = 15 + 20 + 40 = 75
            assertThat(result.getContent().get(2).getCompatibilityScore()).isEqualTo(75);
        }
    }

    @Test
    void findDiscovery_compatibilityScore_clampedToZeroForLargeDistanceAndAgeDifference() {
        LocalDate currentBirthday = LocalDate.now().minusYears(20);
        DatingProfile profile = DatingProfile.builder()
                .id(10L)
                .latitude(BigDecimal.valueOf(21.0285))
                .longitude(BigDecimal.valueOf(105.8542))
                .birthday(currentBirthday)
                .build();

        // Target age is 50 (diff is 30 years -> 100 - 300 = negative -> clamped to 0)
        // Distance is 100km (100 - 200 = negative -> clamped to 0)
        // Common interests is 0
        DatingDiscoveryProjection candidate = mock(DatingDiscoveryProjection.class);
        when(candidate.getUserId()).thenReturn(2L);
        when(candidate.getAge()).thenReturn(50);
        when(candidate.getDistanceKm()).thenReturn(100.0);
        when(candidate.getCommonInterestCount()).thenReturn(0L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<DatingDiscoveryProjection> projectionPage = new PageImpl<>(List.of(candidate));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.findDiscoveryCandidates(USER_ID, pageable)).thenReturn(projectionPage);

            Page<DatingDiscoveryResponse> result = datingDiscoveryService.findDiscovery(pageable);

            assertThat(result.getContent().get(0).getCompatibilityScore()).isEqualTo(0);
        }
    }

    @Test
    void findDiscovery_compatibilityScore_nullAgeAndNullDistance() {
        LocalDate currentBirthday = LocalDate.now().minusYears(25);
        DatingProfile profile = DatingProfile.builder()
                .id(10L)
                .latitude(BigDecimal.valueOf(21.0285))
                .longitude(BigDecimal.valueOf(105.8542))
                .birthday(currentBirthday)
                .build();

        DatingDiscoveryProjection candidate = mock(DatingDiscoveryProjection.class);
        when(candidate.getUserId()).thenReturn(2L);
        when(candidate.getAge()).thenReturn(null); // null age -> 0
        when(candidate.getDistanceKm()).thenReturn(null); // null distance -> 0
        when(candidate.getCommonInterestCount()).thenReturn(null); // null interests -> 0

        Pageable pageable = PageRequest.of(0, 10);
        Page<DatingDiscoveryProjection> projectionPage = new PageImpl<>(List.of(candidate));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.findDiscoveryCandidates(USER_ID, pageable)).thenReturn(projectionPage);

            Page<DatingDiscoveryResponse> result = datingDiscoveryService.findDiscovery(pageable);

            assertThat(result.getContent().get(0).getCompatibilityScore()).isEqualTo(0);
        }
    }
}
