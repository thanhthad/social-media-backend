package media.social.modules.dating.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.preference.UpdateDatingPreferenceRequest;
import media.social.modules.dating.dto.response.preference.DatingPreferenceResponse;
import media.social.modules.dating.entity.DatingPreference;
import media.social.modules.dating.enums.GenderPreference;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.repository.DatingPreferenceRepository;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingPreferenceServiceImplTest {

    @InjectMocks
    private DatingPreferenceServiceImpl datingPreferenceService;

    @Mock
    private DatingPreferenceRepository datingPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    private static final Long USER_ID = 1L;

    // ─────────────────────────────────────────────────────────────────────────
    // createPreference
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createPreference_success_returnsBuiltResponse() {
        UpdateDatingPreferenceRequest request = buildValidRequest(20, 30, GenderPreference.FEMALE, 50);
        User user = buildUser(USER_ID);

        DatingPreference saved = DatingPreference.builder()
                .user(user)
                .minAge(20)
                .maxAge(30)
                .genderPreference(GenderPreference.FEMALE)
                .maxDistance(50)
                .build();
        OffsetDateTime now = OffsetDateTime.now();
        saved.setCreatedAt(now);
        saved.setUpdatedAt(now);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingPreferenceRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(datingPreferenceRepository.save(any(DatingPreference.class))).thenReturn(saved);

            DatingPreferenceResponse response = datingPreferenceService.createPreference(request);

            assertNotNull(response);
            assertEquals(20, response.getMinAge());
            assertEquals(30, response.getMaxAge());
            assertEquals(GenderPreference.FEMALE, response.getGenderPreference());
            assertEquals(50, response.getMaxDistance());

            verify(datingPreferenceRepository).existsByUserId(USER_ID);
            verify(userRepository).findById(USER_ID);
            verify(datingPreferenceRepository).save(any(DatingPreference.class));
        }
    }

    @Test
    void createPreference_alreadyExists_throwsBadRequestException() {
        UpdateDatingPreferenceRequest request = buildValidRequest(20, 30, GenderPreference.MALE, 100);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingPreferenceRepository.existsByUserId(USER_ID)).thenReturn(true);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> datingPreferenceService.createPreference(request));

            assertEquals("Dating preference already exists", ex.getMessage());

            verify(datingPreferenceRepository).existsByUserId(USER_ID);
            verify(userRepository, never()).findById(any());
            verify(datingPreferenceRepository, never()).save(any());
        }
    }

    @Test
    void createPreference_invalidAge_minGreaterThanMax_throwsBadRequestException() {
        UpdateDatingPreferenceRequest request = buildValidRequest(35, 25, GenderPreference.MALE, 100);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingPreferenceRepository.existsByUserId(USER_ID)).thenReturn(false);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> datingPreferenceService.createPreference(request));

            assertEquals("Minimum age cannot greater than maximum age", ex.getMessage());

            verify(userRepository, never()).findById(any());
            verify(datingPreferenceRepository, never()).save(any());
        }
    }

    @Test
    void createPreference_userNotFound_throwsUserNotFoundException() {
        UpdateDatingPreferenceRequest request = buildValidRequest(20, 30, GenderPreference.FEMALE, 50);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingPreferenceRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> datingPreferenceService.createPreference(request));

            verify(userRepository).findById(USER_ID);
            verify(datingPreferenceRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getMyPreference
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getMyPreference_existingPreference_returnsPreference() {
        User user = buildUser(USER_ID);
        OffsetDateTime now = OffsetDateTime.now();
        DatingPreference existing = DatingPreference.builder()
                .user(user)
                .minAge(18)
                .maxAge(28)
                .genderPreference(GenderPreference.MALE)
                .maxDistance(30)
                .build();
        existing.setCreatedAt(now);
        existing.setUpdatedAt(now);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(user);
            when(datingPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

            DatingPreferenceResponse response = datingPreferenceService.getMyPreference();

            assertNotNull(response);
            assertEquals(18, response.getMinAge());
            assertEquals(28, response.getMaxAge());
            assertEquals(GenderPreference.MALE, response.getGenderPreference());
            assertEquals(30, response.getMaxDistance());
            assertEquals(now, response.getCreatedAt());
            assertEquals(now, response.getUpdatedAt());

            verify(userServiceDomain).getByUserId(USER_ID);
            verify(datingPreferenceRepository).findByUserId(USER_ID);
            verify(datingPreferenceRepository, never()).save(any());
        }
    }

    @Test
    void getMyPreference_noExistingPreference_createsDefaultAndReturns() {
        User user = buildUser(USER_ID);
        DatingPreference defaultPreference = DatingPreference.builder()
                .user(user)
                .build();

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(user);
            when(datingPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(datingPreferenceRepository.save(any(DatingPreference.class))).thenReturn(defaultPreference);

            DatingPreferenceResponse response = datingPreferenceService.getMyPreference();

            assertNotNull(response);
            assertNull(response.getMinAge());
            assertNull(response.getMaxAge());
            assertNull(response.getGenderPreference());
            assertNull(response.getMaxDistance());

            verify(userServiceDomain).getByUserId(USER_ID);
            verify(datingPreferenceRepository).findByUserId(USER_ID);
            verify(datingPreferenceRepository).save(any(DatingPreference.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updatePreference
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updatePreference_success_updatesAllFieldsAndReturnsResponse() {
        UpdateDatingPreferenceRequest request = buildValidRequest(22, 40, GenderPreference.FEMALE, 75);
        User user = buildUser(USER_ID);
        OffsetDateTime now = OffsetDateTime.now();

        DatingPreference existing = DatingPreference.builder()
                .user(user)
                .minAge(18)
                .maxAge(30)
                .genderPreference(GenderPreference.MALE)
                .maxDistance(20)
                .build();
        existing.setCreatedAt(now);
        existing.setUpdatedAt(now);

        DatingPreference afterSave = DatingPreference.builder()
                .user(user)
                .minAge(22)
                .maxAge(40)
                .genderPreference(GenderPreference.FEMALE)
                .maxDistance(75)
                .build();
        afterSave.setCreatedAt(now);
        afterSave.setUpdatedAt(now);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(userServiceDomain.getByUserId(USER_ID)).thenReturn(user);
            when(datingPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(datingPreferenceRepository.save(any(DatingPreference.class))).thenReturn(afterSave);

            DatingPreferenceResponse response = datingPreferenceService.updatePreference(request);

            assertNotNull(response);
            assertEquals(22, response.getMinAge());
            assertEquals(40, response.getMaxAge());
            assertEquals(GenderPreference.FEMALE, response.getGenderPreference());
            assertEquals(75, response.getMaxDistance());

            verify(datingPreferenceRepository).findByUserId(USER_ID);
            verify(datingPreferenceRepository).save(any(DatingPreference.class));
        }
    }

    @Test
    void updatePreference_invalidAge_minGreaterThanMax_throwsBadRequestException() {
        UpdateDatingPreferenceRequest request = buildValidRequest(50, 25, GenderPreference.FEMALE, 100);

        try (MockedStatic<UserContextHolder> mockedHolder = mockStatic(UserContextHolder.class)) {
            mockedHolder.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> datingPreferenceService.updatePreference(request));

            assertEquals("Minimum age cannot greater than maximum age", ex.getMessage());

            verify(datingPreferenceRepository, never()).findByUserId(any());
            verify(datingPreferenceRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private UpdateDatingPreferenceRequest buildValidRequest(int minAge, int maxAge,
                                                            GenderPreference gender, int maxDistance) {
        UpdateDatingPreferenceRequest req = new UpdateDatingPreferenceRequest();
        req.setMinAge(minAge);
        req.setMaxAge(maxAge);
        req.setGenderPreference(gender);
        req.setMaxDistance(maxDistance);
        return req;
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
