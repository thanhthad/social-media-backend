package media.social.modules.user.service;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.file.image.dto.response.UploadFileResponse;
import media.social.modules.file.image.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.projection.UserSearchProjection;
import media.social.modules.user.dto.request.profile.*;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.user.*;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.user.exception.user.UserAlreadyExistsException;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.UserCacheService;
import media.social.modules.user.service.cache.UserProfileCacheService;
import media.social.modules.user.service.domain.BlockPolicyService;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserRoleServiceDomain;
import media.social.modules.user.service.impl.UserServiceImpl;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRoleServiceDomain userRoleServiceDomain;

    @Mock
    private FriendShipDomain friendShipDomain;

    @Mock
    private UserProfileCacheService userProfileCacheService;

    @Mock
    private UserCacheService userCacheService;

    @Mock
    private BlockPolicyService blockPolicyService;

    // =========================================================
    // getMe()
    // =========================================================

    @Test
    void getMe_success() {
        Long userId = 1L;

        PublicUserProfileCacheResponse cache = PublicUserProfileCacheResponse.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .fullName("Test User")
                .bio("Bio here")
                .avatarUrl("http://avatar.url")
                .coverUrl("http://cover.url")
                .gender(Gender.MALE)
                .country("Vietnam")
                .city("HCM")
                .district("Q1")
                .occupation("Dev")
                .company("ABC Corp")
                .education("University")
                .visibility(Visibility.PUBLIC)
                .build();

        FriendshipCountResponse friendCount = FriendshipCountResponse.builder()
                .totalFriends(10L)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userProfileCacheService.getUserProfile(userId)).thenReturn(cache);
            when(userProfileCacheService.getTotalFriend(userId)).thenReturn(friendCount);

            MyProfileResponse response = userService.getMe();

            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals("test@example.com", response.getEmail());
            assertEquals("testuser", response.getUsername());
            assertEquals("Test User", response.getFullName());
            assertEquals("Bio here", response.getBio());
            assertEquals(10L, response.getTotalFriend());
            assertEquals("http://avatar.url", response.getAvatarUrl());
            assertEquals("http://cover.url", response.getCoverUrl());
            assertEquals(Gender.MALE, response.getGender());
            assertEquals("Vietnam", response.getCountry());
            assertEquals("HCM", response.getCity());
            assertEquals("Q1", response.getDistrict());
            assertEquals("Dev", response.getOccupation());
            assertEquals("ABC Corp", response.getCompany());
            assertEquals("University", response.getEducation());
            assertEquals(Visibility.PUBLIC, response.getVisibility());
        }
    }

    // =========================================================
    // updateBasicProfile()
    // =========================================================

    @Test
    void updateBasicProfile_success() {
        Long userId = 1L;

        UpdateBasicProfileRequest request = new UpdateBasicProfileRequest();
        request.setFullName("Nguyen Van A");
        request.setBio("Java Developer");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));
        request.setGender(Gender.MALE);

        Profile profile = new Profile();
        User user = new User();
        user.setProfile(profile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            ProfileResponse response = userService.updateBasicProfile(request);

            assertEquals("Nguyen Van A", profile.getFullName());
            assertEquals("Java Developer", profile.getBio());
            assertEquals(LocalDate.of(2000, 1, 1), profile.getDateOfBirth());
            assertEquals(Gender.MALE, profile.getGender());

            assertEquals("Nguyen Van A", response.getFullName());
            assertEquals("Java Developer", response.getBio());
            assertEquals(LocalDate.of(2000, 1, 1), response.getDateOfBirth());
            assertEquals(Gender.MALE, response.getGender());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateBasicProfile_userNotFound() {
        Long userId = 1L;
        UpdateBasicProfileRequest request = new UpdateBasicProfileRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateBasicProfile(request));

            verify(profileRepository, never()).save(any());
            verify(userCacheService, never()).evictProfile(anyLong());
        }
    }

    // =========================================================
    // updateContact()
    // =========================================================

    @Test
    void updateContact_success() {
        Long userId = 1L;

        UpdateContactRequest request = new UpdateContactRequest();
        request.setPhone("0901234567");
        request.setWebsite("https://example.com");
        request.setCountry("Vietnam");
        request.setCity("Ho Chi Minh");
        request.setDistrict("Quan 1");

        Profile profile = new Profile();
        User user = new User();
        user.setProfile(profile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            ProfileResponse response = userService.updateContact(request);

            assertEquals("0901234567", profile.getPhone());
            assertEquals("https://example.com", profile.getWebsite());
            assertEquals("Vietnam", profile.getCountry());
            assertEquals("Ho Chi Minh", profile.getCity());
            assertEquals("Quan 1", profile.getDistrict());

            assertEquals("0901234567", response.getPhone());
            assertEquals("https://example.com", response.getWebsite());
            assertEquals("Vietnam", response.getCountry());
            assertEquals("Ho Chi Minh", response.getCity());
            assertEquals("Quan 1", response.getDistrict());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateContact_userNotFound() {
        Long userId = 1L;
        UpdateContactRequest request = new UpdateContactRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateContact(request));

            verify(profileRepository, never()).save(any());
            verify(userCacheService, never()).evictProfile(anyLong());
        }
    }

    // =========================================================
    // updateCareer()
    // =========================================================

    @Test
    void updateCareer_success() {
        Long userId = 1L;

        UpdateCareerRequest request = new UpdateCareerRequest();
        request.setOccupation("Software Engineer");
        request.setCompany("Google");
        request.setEducation("Bach Khoa University");

        Profile profile = new Profile();
        User user = new User();
        user.setProfile(profile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            ProfileResponse response = userService.updateCareer(request);

            assertEquals("Software Engineer", profile.getOccupation());
            assertEquals("Google", profile.getCompany());
            assertEquals("Bach Khoa University", profile.getEducation());

            assertEquals("Software Engineer", response.getOccupation());
            assertEquals("Google", response.getCompany());
            assertEquals("Bach Khoa University", response.getEducation());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateCareer_userNotFound() {
        Long userId = 1L;
        UpdateCareerRequest request = new UpdateCareerRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateCareer(request));

            verify(profileRepository, never()).save(any());
            verify(userCacheService, never()).evictProfile(anyLong());
        }
    }

    // =========================================================
    // updateSocialLinks()
    // =========================================================

    @Test
    void updateSocialLinks_success() {
        Long userId = 1L;

        Map<String, String> links = Map.of(
                "github", "https://github.com/user",
                "linkedin", "https://linkedin.com/in/user"
        );

        UpdateSocialLinksRequest request = new UpdateSocialLinksRequest();
        request.setSocialLinks(links);

        Profile profile = new Profile();
        User user = new User();
        user.setProfile(profile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            ProfileResponse response = userService.updateSocialLinks(request);

            assertEquals(links, profile.getSocialLinks());
            assertEquals(links, response.getSocialLinks());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateSocialLinks_userNotFound() {
        Long userId = 1L;
        UpdateSocialLinksRequest request = new UpdateSocialLinksRequest();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateSocialLinks(request));

            verify(profileRepository, never()).save(any());
            verify(userCacheService, never()).evictProfile(anyLong());
        }
    }

    // =========================================================
    // updateProfileVisibility()
    // =========================================================

    @Test
    void updateProfileVisibility_success() {
        Long userId = 1L;

        UpdateProfileVisibilityRequest request = new UpdateProfileVisibilityRequest();
        request.setVisibility(Visibility.FRIEND);

        Profile profile = new Profile();
        User user = new User();
        user.setProfile(profile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            ProfileResponse response = userService.updateProfileVisibility(request);

            assertEquals(Visibility.FRIEND, profile.getVisibility());
            assertEquals(Visibility.FRIEND, response.getProfileVisibility());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateProfileVisibility_userNotFound() {
        Long userId = 1L;
        UpdateProfileVisibilityRequest request = new UpdateProfileVisibilityRequest();
        request.setVisibility(Visibility.PRIVATE);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateProfileVisibility(request));

            verify(profileRepository, never()).save(any());
            verify(userCacheService, never()).evictProfile(anyLong());
        }
    }

    // =========================================================
    // updateUserName()
    // =========================================================

    @Test
    void updateUserName_success() {
        Long userId = 1L;

        UpdateUsernameRequest request = new UpdateUsernameRequest();
        request.setUserName("new_username");

        User user = new User();
        user.setId(userId);
        user.setEmailVerified(false);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userRepository.existsByUsername("new_username")).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.updateUserName(request);

            assertEquals("new_username", user.getUsername());
            assertEquals("new_username", response.getUsername());
            verify(userRepository).save(user);
        }
    }

    @Test
    void updateUserName_usernameAlreadyExists_throwsException() {
        UpdateUsernameRequest request = new UpdateUsernameRequest();
        request.setUserName("existing_username");

        when(userRepository.existsByUsername("existing_username")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.updateUserName(request));

        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserName_userNotFound_throwsException() {
        Long userId = 1L;

        UpdateUsernameRequest request = new UpdateUsernameRequest();
        request.setUserName("new_username");

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.existsByUsername("new_username")).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateUserName(request));

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updateUserName_emailAlreadyVerified_throwsAccessDenied() {
        Long userId = 1L;

        UpdateUsernameRequest request = new UpdateUsernameRequest();
        request.setUserName("new_username");

        User user = new User();
        user.setId(userId);
        user.setEmailVerified(true);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.existsByUsername("new_username")).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThrows(AccessDeniedException.class,
                    () -> userService.updateUserName(request));

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================
    // updateCover()
    // =========================================================

    @Test
    void updateCover_success_withExistingCover() {
        Long userId = 1L;

        MultipartFile mockFile = mock(MultipartFile.class);
        UpdateCoverRequest request = new UpdateCoverRequest(mockFile);

        Profile profile = new Profile();
        profile.setCoverPublicId("old-cover-public-id");
        User user = new User();
        user.setProfile(profile);

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("http://new-cover.url")
                .publicId("new-cover-public-id")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
            when(cloudinaryService.uploadFile(mockFile, "covers", MediaType.IMAGE))
                    .thenReturn(uploadResponse);

            ProfileResponse response = userService.updateCover(request);

            verify(cloudinaryService).validateFile(mockFile, MediaType.IMAGE);
            verify(cloudinaryService).deleteFile("old-cover-public-id", MediaType.IMAGE);
            verify(cloudinaryService).uploadFile(mockFile, "covers", MediaType.IMAGE);

            assertEquals("http://new-cover.url", profile.getCoverUrl());
            assertEquals("new-cover-public-id", profile.getCoverPublicId());
            assertEquals("http://new-cover.url", response.getCoverUrl());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateCover_success_withoutExistingCover() {
        Long userId = 1L;

        MultipartFile mockFile = mock(MultipartFile.class);
        UpdateCoverRequest request = new UpdateCoverRequest(mockFile);

        Profile profile = new Profile();
        User user = new User();
        user.setProfile(profile);

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("http://new-cover.url")
                .publicId("new-cover-public-id")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
            when(cloudinaryService.uploadFile(mockFile, "covers", MediaType.IMAGE))
                    .thenReturn(uploadResponse);

            ProfileResponse response = userService.updateCover(request);

            verify(cloudinaryService, never()).deleteFile(any(), any());
            assertEquals("http://new-cover.url", response.getCoverUrl());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateCover_userNotFound() {
        Long userId = 1L;
        MultipartFile mockFile = mock(MultipartFile.class);
        UpdateCoverRequest request = new UpdateCoverRequest(mockFile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateCover(request));

            verify(cloudinaryService, never()).uploadFile(any(), any(), any());
            verify(profileRepository, never()).save(any());
        }
    }

    // =========================================================
    // updateAvatar()
    // =========================================================

    @Test
    void updateAvatar_success_withExistingAvatar() {
        Long userId = 1L;

        MultipartFile mockFile = mock(MultipartFile.class);
        UpdateAvatarRequest request = new UpdateAvatarRequest(mockFile);

        Profile profile = new Profile();
        profile.setAvatarPublicId("old-avatar-public-id");

        User user = new User();
        user.setId(userId);
        user.setProfile(profile);

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("http://new-avatar.url")
                .publicId("new-avatar-public-id")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
            when(cloudinaryService.uploadFile(mockFile, "avatars", MediaType.IMAGE))
                    .thenReturn(uploadResponse);

            ProfileResponse response = userService.updateAvatar(request);

            verify(cloudinaryService).validateFile(mockFile, MediaType.IMAGE);
            verify(cloudinaryService).deleteFile("old-avatar-public-id", MediaType.IMAGE);
            verify(cloudinaryService).uploadFile(mockFile, "avatars", MediaType.IMAGE);

            assertEquals("http://new-avatar.url", profile.getAvatarUrl());
            assertEquals("new-avatar-public-id", profile.getAvatarPublicId());
            assertEquals("http://new-avatar.url", response.getAvatarUrl());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateAvatar_success_withoutExistingAvatar() {
        Long userId = 1L;

        MultipartFile mockFile = mock(MultipartFile.class);
        UpdateAvatarRequest request = new UpdateAvatarRequest(mockFile);

        Profile profile = new Profile();
        User user = new User();
        user.setId(userId);
        user.setProfile(profile);

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("http://new-avatar.url")
                .publicId("new-avatar-public-id")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
            when(cloudinaryService.uploadFile(mockFile, "avatars", MediaType.IMAGE))
                    .thenReturn(uploadResponse);

            ProfileResponse response = userService.updateAvatar(request);

            verify(cloudinaryService, never()).deleteFile(any(), any());
            assertEquals("http://new-avatar.url", response.getAvatarUrl());

            verify(profileRepository).save(profile);
            verify(userCacheService).evictProfile(userId);
        }
    }

    @Test
    void updateAvatar_userNotFound() {
        Long userId = 1L;
        MultipartFile mockFile = mock(MultipartFile.class);
        UpdateAvatarRequest request = new UpdateAvatarRequest(mockFile);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateAvatar(request));

            verify(cloudinaryService, never()).uploadFile(any(), any(), any());
            verify(profileRepository, never()).save(any());
        }
    }

    // =========================================================
    // updatePassword()
    // =========================================================

    @Test
    void updatePassword_success() {
        Long userId = 1L;

        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "newPass456");

        User user = new User();
        user.setId(userId);
        user.setProvider(AuthProvider.LOCAL);
        user.setPasswordHash("hashed_old_pass");

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", "hashed_old_pass")).thenReturn(true);
            when(passwordEncoder.encode("newPass456")).thenReturn("hashed_new_pass");

            userService.updatePassword(request);

            assertEquals("hashed_new_pass", user.getPasswordHash());
            verify(userRepository).save(user);
        }
    }

    @Test
    void updatePassword_userNotFound() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "newPass456");

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updatePassword(request));

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updatePassword_googleUser_throwsBadCredentials() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "newPass456");

        User user = new User();
        user.setProvider(AuthProvider.GOOGLE);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThrows(BadCredentialsException.class,
                    () -> userService.updatePassword(request));

            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updatePassword_sameAsOldPassword_throwsBadCredentials() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("samePass123", "samePass123");

        User user = new User();
        user.setProvider(AuthProvider.LOCAL);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThrows(BadCredentialsException.class,
                    () -> userService.updatePassword(request));

            verify(passwordEncoder, never()).matches(any(), any());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updatePassword_wrongOldPassword_throwsBadCredentials() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOld", "newPass456");

        User user = new User();
        user.setProvider(AuthProvider.LOCAL);
        user.setPasswordHash("hashed_real_pass");

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongOld", "hashed_real_pass")).thenReturn(false);

            assertThrows(BadCredentialsException.class,
                    () -> userService.updatePassword(request));

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================
    // getUserById()
    // =========================================================

    @Test
    void getUserById_success_publicProfile() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        PublicUserProfileCacheResponse cache = PublicUserProfileCacheResponse.builder()
                .id(targetUserId)
                .username("target_user")
                .avatarUrl("http://avatar.url")
                .coverUrl("http://cover.url")
                .fullName("Target User")
                .bio("Bio")
                .visibility(Visibility.PUBLIC)
                .build();

        FriendshipCountResponse friendCount = FriendshipCountResponse.builder()
                .totalFriends(5L)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(true);
            when(userProfileCacheService.getUserProfile(targetUserId)).thenReturn(cache);
            when(userProfileCacheService.getTotalFriend(targetUserId)).thenReturn(friendCount);
            when(friendShipDomain.areFriends(currentUserId, targetUserId)).thenReturn(false);

            PublicProfileResponse response = userService.getUserById(targetUserId);

            assertNotNull(response);
            assertEquals(targetUserId, response.getUserId());
            assertEquals("target_user", response.getUsername());
            assertEquals("Target User", response.getFullName());
            assertEquals("Bio", response.getBio());
            assertEquals(5L, response.getTotalFriend());
            assertFalse(response.isFriend());
        }
    }

    @Test
    void getUserById_sameUser_throwsUserAlreadyExistsException() {
        Long userId = 1L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            assertThrows(UserAlreadyExistsException.class,
                    () -> userService.getUserById(userId));

            verify(blockPolicyService, never()).isBlocked(anyLong(), anyLong());
        }
    }

    @Test
    void getUserById_isBlocked_throwsUserBlockedException() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(true);

            assertThrows(UserBlockedException.class,
                    () -> userService.getUserById(targetUserId));
        }
    }

    @Test
    void getUserById_moderatorViewsAdminProfile_throwsAccessDenied() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.MODERATOR)).thenReturn(true);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(true);

            assertThrows(AccessDeniedException.class,
                    () -> userService.getUserById(targetUserId));
        }
    }

    @Test
    void getUserById_normalUserViewsAdminProfile_throwsAccessDenied() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(true);

            assertThrows(AccessDeniedException.class,
                    () -> userService.getUserById(targetUserId));
        }
    }

    @Test
    void getUserById_normalUserViewsModeratorProfile_throwsAccessDenied() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(true);

            assertThrows(AccessDeniedException.class,
                    () -> userService.getUserById(targetUserId));
        }
    }

    @Test
    void getUserById_friendProfile_canViewFullProfile() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        PublicUserProfileCacheResponse cache = PublicUserProfileCacheResponse.builder()
                .id(targetUserId)
                .username("friend_user")
                .fullName("Friend User")
                .bio("Friend Bio")
                .visibility(Visibility.FRIEND)
                .build();

        FriendshipCountResponse friendCount = FriendshipCountResponse.builder()
                .totalFriends(3L)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userProfileCacheService.getUserProfile(targetUserId)).thenReturn(cache);
            when(userProfileCacheService.getTotalFriend(targetUserId)).thenReturn(friendCount);
            when(friendShipDomain.areFriends(currentUserId, targetUserId)).thenReturn(true);

            PublicProfileResponse response = userService.getUserById(targetUserId);

            assertNotNull(response);
            assertEquals("Friend User", response.getFullName());
            assertEquals("Friend Bio", response.getBio());
            assertTrue(response.isFriend());
        }
    }

    @Test
    void getUserById_privateProfile_hidesFullDetails() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        PublicUserProfileCacheResponse cache = PublicUserProfileCacheResponse.builder()
                .id(targetUserId)
                .username("private_user")
                .fullName("Private User")
                .bio("Private Bio")
                .visibility(Visibility.PRIVATE)
                .build();

        FriendshipCountResponse friendCount = FriendshipCountResponse.builder()
                .totalFriends(0L)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userProfileCacheService.getUserProfile(targetUserId)).thenReturn(cache);
            when(userProfileCacheService.getTotalFriend(targetUserId)).thenReturn(friendCount);
            when(friendShipDomain.areFriends(currentUserId, targetUserId)).thenReturn(false);

            PublicProfileResponse response = userService.getUserById(targetUserId);

            assertNotNull(response);
            // Profile PRIVATE -> không trả fullName, bio
            assertNull(response.getFullName());
            assertNull(response.getBio());
        }
    }

    @Test
    void getUserById_friendVisibility_notFriends_hidesFullDetails() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        PublicUserProfileCacheResponse cache = PublicUserProfileCacheResponse.builder()
                .id(targetUserId)
                .username("friend_only_user")
                .fullName("Friend Only User")
                .bio("Friend Only Bio")
                .visibility(Visibility.FRIEND)
                .build();

        FriendshipCountResponse friendCount = FriendshipCountResponse.builder()
                .totalFriends(5L)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(blockPolicyService.isBlocked(currentUserId, targetUserId)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(currentUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(false);
            when(userProfileCacheService.getUserProfile(targetUserId)).thenReturn(cache);
            when(userProfileCacheService.getTotalFriend(targetUserId)).thenReturn(friendCount);
            when(friendShipDomain.areFriends(currentUserId, targetUserId)).thenReturn(false);

            PublicProfileResponse response = userService.getUserById(targetUserId);

            assertNotNull(response);
            // Visibility FRIEND nhưng không phải bạn bè -> không trả fullName, bio
            assertNull(response.getFullName());
            assertNull(response.getBio());
            assertEquals("friend_only_user", response.getUsername());
            assertEquals(5L, response.getTotalFriend());
            assertFalse(response.isFriend());
        }
    }

    // =========================================================
    // findUsersByName()
    // =========================================================

    @Test
    void findUsersByName_success() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        UserSearchProjection projection = new UserSearchProjection() {
            @Override public Long getId() { return 2L; }
            @Override public String getUsername() { return "found_user"; }
            @Override public String getAvatarUrl() { return "http://avatar.url"; }
            @Override public String getFullName() { return "Found User"; }
        };

        Page<UserSearchProjection> projectionPage = new PageImpl<>(List.of(projection));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);
            when(userRepository.searchUsers("found", Status.ACTIVE.name(), viewerId, pageable))
                    .thenReturn(projectionPage);

            Page<UserSearchResponse> result = userService.findUsersByName("found", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            UserSearchResponse first = result.getContent().get(0);
            assertEquals(2L, first.getId());
            assertEquals("found_user", first.getUsername());
            assertEquals("http://avatar.url", first.getAvatarUrl());
            assertEquals("Found User", first.getFullName());
        }
    }

    @Test
    void findUsersByName_returnsEmptyPage() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<UserSearchProjection> emptyPage = new PageImpl<>(List.of());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);
            when(userRepository.searchUsers("nonexistent", Status.ACTIVE.name(), viewerId, pageable))
                    .thenReturn(emptyPage);

            Page<UserSearchResponse> result = userService.findUsersByName("nonexistent", pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}