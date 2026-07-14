package media.social.user.service;

import media.social.modults.file.image.dto.response.UploadFileResponse;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.user.dto.request.user.ChangePasswordRequest;
import media.social.modults.user.dto.request.user.UpdateAvatarRequest;
import media.social.modults.user.dto.response.user.UserProfileResponse;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.mapper.UserMapper;
import media.social.modults.user.repository.ProfileRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private ProfileRepository profileRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Profile testProfile;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).username("testuser").passwordHash("encodedOldPass").build();
        testProfile = Profile.builder().user(testUser).avatarPublicId("old_id").build();
    }

    @Test
    @DisplayName("updateAvatar - Should update successfully and delete old image")
    void updateAvatar_Success() {
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
            UpdateAvatarRequest request = new UpdateAvatarRequest(file);
            UploadFileResponse uploadResponse = new UploadFileResponse("new_url", "new_id");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfile));
            when(cloudinaryService.uploadImage(any(), anyString())).thenReturn(uploadResponse);
            when(userMapper.toUserProfileResponse(any(), any())).thenReturn(new UserProfileResponse());

            userService.updateAvatar(request);

            verify(cloudinaryService).deleteImage("old_id");
            verify(profileRepository).save(testProfile);
            assertThat(testProfile.getAvatarUrl()).isEqualTo("new_url");
        }
    }

    @Test
    @DisplayName("updatePassword - Should throw exception if old password invalid")
    void updatePassword_InvalidOldPassword() {
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newPass");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong", "encodedOldPass")).thenReturn(false);

            assertThrows(BadCredentialsException.class, () -> userService.updatePassword(request));
        }
    }

    @Test
    @DisplayName("updatePassword - Should throw exception if new password same as old")
    void updatePassword_SamePassword() {
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            ChangePasswordRequest request = new ChangePasswordRequest("old", "old");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            assertThrows(BadCredentialsException.class, () -> userService.updatePassword(request));
        }
    }

    @Test
    @DisplayName("getMe - Should throw UserNotFoundException when user does not exist")
    void getMe_UserNotFound() {
        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(99L);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getMe());
        }
    }
}