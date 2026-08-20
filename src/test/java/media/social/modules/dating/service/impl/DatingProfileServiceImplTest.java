package media.social.modules.dating.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.profile.*;
import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.dto.response.profile.DatingProfileResponse;
import media.social.modules.dating.dto.response.profile.MyDatingProfileResponse;
import media.social.modules.dating.dto.response.profile.PublicDatingProfileResponse;
import media.social.modules.dating.dto.response.projection.DatingDistanceProjection;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.service.cache.DatingProfileCacheService;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingProfileServiceImplTest {

    @Mock
    private DatingProfileRepository datingProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DatingProfileCacheService datingProfileCacheService;

    @InjectMocks
    private DatingProfileServiceImpl datingProfileServiceImpl;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    // ---------------------------------------------------------------------------
    // Helper: build a fully-populated DatingProfileCacheResponse (no builder)
    // ---------------------------------------------------------------------------
    private DatingProfileCacheResponse buildCacheResponse(Visibility visibility) {
        return new DatingProfileCacheResponse(
                "john_doe",
                "https://example.com/avatar.png",
                "https://example.com/cover.png",
                "John Doe",
                "Hello world",
                null,
                LocalDate.of(1995, 6, 15),
                175,
                "Engineer",
                "Bachelor",
                "Vietnam",
                "Hanoi",
                "Ba Dinh",
                true,
                visibility,
                OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private DatingProfileCacheResponse buildCacheResponse() {
        return buildCacheResponse(Visibility.PUBLIC);
    }

    // ---------------------------------------------------------------------------
    // Helper: build a DatingProfile entity via builder
    // ---------------------------------------------------------------------------
    private DatingProfile buildDatingProfile() {
        return DatingProfile.builder()
                .id(10L)
                .displayName("John Doe")
                .bio("Hello world")
                .gender(null)
                .birthday(LocalDate.of(1995, 6, 15))
                .height(175)
                .occupation("Engineer")
                .education("Bachelor")
                .latitude(new BigDecimal("21.0285"))
                .longitude(new BigDecimal("105.8542"))
                .country("Vietnam")
                .city("Hanoi")
                .district("Ba Dinh")
                .active(true)
                .visibility(Visibility.PUBLIC)
                .build();
    }

    // ===========================================================================
    // getMe
    // ===========================================================================

    @Test
    void getMe_success() {
        DatingProfileCacheResponse cacheResponse = buildCacheResponse();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileCacheService.getDatingProfile(USER_ID)).thenReturn(cacheResponse);

            MyDatingProfileResponse result = datingProfileServiceImpl.getMe();

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(cacheResponse.getUsername());
            assertThat(result.getAvatarUrl()).isEqualTo(cacheResponse.getAvatarUrl());
            assertThat(result.getCoverUrl()).isEqualTo(cacheResponse.getCoverUrl());
            assertThat(result.getDisplayName()).isEqualTo(cacheResponse.getDisplayName());
            assertThat(result.getBio()).isEqualTo(cacheResponse.getBio());
            assertThat(result.getBirthday()).isEqualTo(cacheResponse.getBirthday());
            assertThat(result.getHeight()).isEqualTo(cacheResponse.getHeight());
            assertThat(result.getOccupation()).isEqualTo(cacheResponse.getOccupation());
            assertThat(result.getEducation()).isEqualTo(cacheResponse.getEducation());
            assertThat(result.getCountry()).isEqualTo(cacheResponse.getCountry());
            assertThat(result.getCity()).isEqualTo(cacheResponse.getCity());
            assertThat(result.getDistrict()).isEqualTo(cacheResponse.getDistrict());
            assertThat(result.getActive()).isEqualTo(cacheResponse.getActive());
            assertThat(result.getVisibility()).isEqualTo(cacheResponse.getVisibility());
            assertThat(result.getCreatedAt()).isEqualTo(cacheResponse.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(cacheResponse.getUpdatedAt());

            verify(datingProfileCacheService).getDatingProfile(USER_ID);
        }
    }

    // ===========================================================================
    // updateBasicInfo
    // ===========================================================================

    @Test
    void updateBasicInfo_success_verifyFieldsSetCacheEvictedAndSaved() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingBasicInfoRequest request = new UpdateDatingBasicInfoRequest();
        request.setDisplayName("Jane Doe");
        request.setGender(null);
        request.setBirthday(LocalDate.of(1998, 3, 20));
        request.setHeight(160);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateBasicInfo(request);

            assertThat(result).isNotNull();
            assertThat(result.getDisplayName()).isEqualTo("Jane Doe");
            assertThat(result.getBirthday()).isEqualTo(LocalDate.of(1998, 3, 20));
            assertThat(result.getHeight()).isEqualTo(160);

            verify(datingProfileRepository).save(profile);
            verify(datingProfileCacheService).evictProfile(USER_ID);
        }
    }

    @Test
    void updateBasicInfo_profileNotFound_throwsDatingProfileNotFoundException() {
        UpdateDatingBasicInfoRequest request = new UpdateDatingBasicInfoRequest();
        request.setDisplayName("Jane Doe");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(DatingProfileNotFoundException.class,
                    () -> datingProfileServiceImpl.updateBasicInfo(request));

            verify(datingProfileRepository, never()).save(any());
            verify(datingProfileCacheService, never()).evictProfile(any());
        }
    }

    // ===========================================================================
    // updateCareer
    // ===========================================================================

    @Test
    void updateCareer_success() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingCareerRequest request = new UpdateDatingCareerRequest();
        request.setOccupation("Doctor");
        request.setEducation("Master");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateCareer(request);

            assertThat(result).isNotNull();
            assertThat(result.getOccupation()).isEqualTo("Doctor");
            assertThat(result.getEducation()).isEqualTo("Master");

            verify(datingProfileCacheService).evictProfile(USER_ID);
            verify(datingProfileRepository).save(profile);
        }
    }

    // ===========================================================================
    // updateLocation
    // ===========================================================================

    @Test
    void updateLocation_success() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingLocationRequest request = new UpdateDatingLocationRequest();
        request.setCountry("Japan");
        request.setCity("Tokyo");
        request.setDistrict("Shinjuku");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateLocation(request);

            assertThat(result).isNotNull();
            assertThat(result.getCountry()).isEqualTo("Japan");
            assertThat(result.getCity()).isEqualTo("Tokyo");
            assertThat(result.getDistrict()).isEqualTo("Shinjuku");

            verify(datingProfileRepository).save(profile);
            verify(datingProfileCacheService).evictProfile(USER_ID);
        }
    }

    // ===========================================================================
    // updateCoordinates
    // ===========================================================================

    @Test
    void updateCoordinates_success() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingCoordinatesRequest request = new UpdateDatingCoordinatesRequest();
        request.setLatitude(new BigDecimal("35.6762"));
        request.setLongitude(new BigDecimal("139.6503"));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateCoordinates(request);

            assertThat(result).isNotNull();
            assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("35.6762"));
            assertThat(result.getLongitude()).isEqualByComparingTo(new BigDecimal("139.6503"));

            verify(datingProfileRepository).save(profile);
            // updateCoordinates does NOT evict cache
            verify(datingProfileCacheService, never()).evictProfile(any());
        }
    }

    // ===========================================================================
    // updateBio
    // ===========================================================================

    @Test
    void updateBio_success() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingBioRequest request = new UpdateDatingBioRequest();
        request.setBio("New bio text");

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateBio(request);

            assertThat(result).isNotNull();
            assertThat(result.getBio()).isEqualTo("New bio text");

            verify(datingProfileCacheService).evictProfile(USER_ID);
            verify(datingProfileRepository).save(profile);
        }
    }

    // ===========================================================================
    // updateVisibility
    // ===========================================================================

    @Test
    void updateVisibility_success() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingVisibilityRequest request = new UpdateDatingVisibilityRequest();
        request.setVisibility(Visibility.PRIVATE);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateVisibility(request);

            assertThat(result).isNotNull();
            assertThat(result.getVisibility()).isEqualTo(Visibility.PRIVATE);

            verify(datingProfileCacheService).evictProfile(USER_ID);
            verify(datingProfileRepository).save(profile);
        }
    }

    // ===========================================================================
    // updateStatus
    // ===========================================================================

    @Test
    void updateStatus_success() {
        DatingProfile profile = buildDatingProfile();

        UpdateDatingStatusRequest request = new UpdateDatingStatusRequest();
        request.setActive(false);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(datingProfileRepository.save(profile)).thenReturn(profile);

            DatingProfileResponse result = datingProfileServiceImpl.updateStatus(request);

            assertThat(result).isNotNull();
            assertThat(result.getActive()).isFalse();

            verify(datingProfileCacheService).evictProfile(USER_ID);
            verify(datingProfileRepository).save(profile);
        }
    }

    // ===========================================================================
    // getPublicProfile
    // ===========================================================================

    @Test
    void getPublicProfile_ownProfile_returnsFullProfile() {
        // currentUserId == userId → condition !currentUserId.equals(userId) is false
        // → always returns full profile regardless of visibility
        DatingProfileCacheResponse cacheResponse = buildCacheResponse(Visibility.PRIVATE);

        DatingDistanceProjection distanceProjection = mock(DatingDistanceProjection.class);
        when(distanceProjection.getDistanceKm()).thenReturn(0.0);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileCacheService.getDatingProfile(USER_ID)).thenReturn(cacheResponse);
            when(datingProfileRepository.findDistanceBetweenUsers(USER_ID, USER_ID))
                    .thenReturn(distanceProjection);

            PublicDatingProfileResponse result = datingProfileServiceImpl.getPublicProfile(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(cacheResponse.getUsername());
            assertThat(result.getDisplayName()).isEqualTo(cacheResponse.getDisplayName());
            assertThat(result.getOccupation()).isEqualTo(cacheResponse.getOccupation());
            assertThat(result.getEducation()).isEqualTo(cacheResponse.getEducation());
            assertThat(result.getCountry()).isEqualTo(cacheResponse.getCountry());
            assertThat(result.getCity()).isEqualTo(cacheResponse.getCity());
            assertThat(result.getDistrict()).isEqualTo(cacheResponse.getDistrict());
            assertThat(result.getDistanceKm()).isEqualTo(0.0);

            verify(datingProfileRepository).findDistanceBetweenUsers(USER_ID, USER_ID);
        }
    }

    @Test
    void getPublicProfile_privateProfile_viewingOther_returnsLimitedProfile() {
        // Different user + PRIVATE visibility → limited response only
        DatingProfileCacheResponse cacheResponse = buildCacheResponse(Visibility.PRIVATE);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileCacheService.getDatingProfile(OTHER_USER_ID)).thenReturn(cacheResponse);

            PublicDatingProfileResponse result = datingProfileServiceImpl.getPublicProfile(OTHER_USER_ID);

            assertThat(result).isNotNull();
            // Limited profile contains only basic fields
            assertThat(result.getAvatarUrl()).isEqualTo(cacheResponse.getAvatarUrl());
            assertThat(result.getCoverUrl()).isEqualTo(cacheResponse.getCoverUrl());
            assertThat(result.getDisplayName()).isEqualTo(cacheResponse.getDisplayName());
            assertThat(result.getBirthday()).isEqualTo(cacheResponse.getBirthday());
            assertThat(result.getBio()).isEqualTo(cacheResponse.getBio());
            // Full-profile fields must be absent
            assertThat(result.getUsername()).isNull();
            assertThat(result.getOccupation()).isNull();
            assertThat(result.getEducation()).isNull();
            assertThat(result.getCountry()).isNull();
            assertThat(result.getDistanceKm()).isNull();

            // Must NOT query distance for a private/limited response
            verify(datingProfileRepository, never()).findDistanceBetweenUsers(any(), any());
        }
    }

    @Test
    void getPublicProfile_publicProfile_viewingOther_returnsFullProfile() {
        // Different user + PUBLIC visibility → full profile with distance
        DatingProfileCacheResponse cacheResponse = buildCacheResponse(Visibility.PUBLIC);

        DatingDistanceProjection distanceProjection = mock(DatingDistanceProjection.class);
        when(distanceProjection.getDistanceKm()).thenReturn(12.5);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileCacheService.getDatingProfile(OTHER_USER_ID)).thenReturn(cacheResponse);
            when(datingProfileRepository.findDistanceBetweenUsers(USER_ID, OTHER_USER_ID))
                    .thenReturn(distanceProjection);

            PublicDatingProfileResponse result = datingProfileServiceImpl.getPublicProfile(OTHER_USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(cacheResponse.getUsername());
            assertThat(result.getDisplayName()).isEqualTo(cacheResponse.getDisplayName());
            assertThat(result.getBio()).isEqualTo(cacheResponse.getBio());
            assertThat(result.getOccupation()).isEqualTo(cacheResponse.getOccupation());
            assertThat(result.getEducation()).isEqualTo(cacheResponse.getEducation());
            assertThat(result.getCountry()).isEqualTo(cacheResponse.getCountry());
            assertThat(result.getCity()).isEqualTo(cacheResponse.getCity());
            assertThat(result.getDistrict()).isEqualTo(cacheResponse.getDistrict());
            assertThat(result.getDistanceKm()).isEqualTo(12.5);

            verify(datingProfileRepository).findDistanceBetweenUsers(USER_ID, OTHER_USER_ID);
        }
    }

    // ===========================================================================
    // deleteProfile
    // ===========================================================================

    @Test
    void deleteProfile_success_setsActiveToFalse() {
        DatingProfile profile = buildDatingProfile();
        assertThat(profile.getActive()).isTrue(); // precondition

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            datingProfileServiceImpl.deleteProfile();

            assertThat(profile.getActive()).isFalse();
            // The impl only mutates the entity flag — no explicit save() call
            verify(datingProfileRepository, never()).save(any());
        }
    }

    @Test
    void deleteProfile_profileNotFound_throwsDatingProfileNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);
            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(DatingProfileNotFoundException.class,
                    () -> datingProfileServiceImpl.deleteProfile());

            verify(datingProfileRepository, never()).save(any());
        }
    }
}
