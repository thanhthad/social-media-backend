package media.social.modules.dating.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.profile.CreateDatingProfilePhotoRequest;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.entity.DatingProfilePhoto;
import media.social.modules.dating.exception.photo.DatingProfilePhotoAlreadyPrimaryException;
import media.social.modules.dating.exception.photo.DatingProfilePhotoLimitExceededException;
import media.social.modules.dating.exception.photo.DatingProfilePhotoNotFoundException;
import media.social.modules.dating.exception.photo.InvalidDatingProfilePhotoException;
import media.social.modules.dating.exception.photo.UnauthorizedDatingProfilePhotoException;
import media.social.modules.dating.repository.DatingProfilePhotoRepository;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingProfilePhotoServiceImplTest {

    @Mock
    private DatingProfilePhotoRepository datingProfilePhotoRepository;

    @Mock
    private MediaUploadService mediaUploadService;

    @Mock
    private DatingProfileRepository datingProfileRepository;

    @InjectMocks
    private DatingProfilePhotoServiceImpl datingProfilePhotoService;

    private static final Long USER_ID = 1L;
    private static final Long PROFILE_ID = 10L;
    private static final Long PHOTO_ID = 100L;

    private User user;
    private DatingProfile datingProfile;
    private MultipartFile mockFile;
    private CreateDatingProfilePhotoRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .build();

        datingProfile = DatingProfile.builder()
                .id(PROFILE_ID)
                .user(user)
                .build();

        mockFile = mock(MultipartFile.class);

        request = new CreateDatingProfilePhotoRequest();
        request.setFile(mockFile);
    }

    // ─────────────────────────────────────────────
    // createPhoto
    // ─────────────────────────────────────────────

    @Test
    void createPhoto_firstPhoto_isMarkedAsPrimary_displayOrderZero() {
        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("https://cdn.example.com/photo.jpg")
                .publicId("DatingPhoto/abc123")
                .resourceType("IMAGE")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(datingProfile));
            when(datingProfilePhotoRepository.countByDatingProfileId(PROFILE_ID)).thenReturn(0L);
            when(mediaUploadService.upload(mockFile, MediaUploadContext.PROFILE)).thenReturn(uploadResponse);
            when(datingProfilePhotoRepository.findTopByDatingProfileIdOrderByDisplayOrderDesc(PROFILE_ID))
                    .thenReturn(Optional.empty());

            datingProfilePhotoService.createPhoto(request);

            verify(mediaUploadService).upload(mockFile, MediaUploadContext.PROFILE);
            verify(datingProfilePhotoRepository).save(argThat(photo ->
                    Boolean.TRUE.equals(photo.getPrimary())
                            && photo.getDisplayOrder() == 0
                            && "https://cdn.example.com/photo.jpg".equals(photo.getImageUrl())
                            && "DatingPhoto/abc123".equals(photo.getPublicId())
                            && photo.getDatingProfile().equals(datingProfile)
            ));
        }
    }

    @Test
    void createPhoto_subsequentPhoto_notPrimary_incrementsDisplayOrder() {
        DatingProfilePhoto lastPhoto = DatingProfilePhoto.builder()
                .displayOrder(2)
                .build();

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("https://cdn.example.com/photo2.jpg")
                .publicId("DatingPhoto/xyz789")
                .resourceType("IMAGE")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(datingProfile));
            when(datingProfilePhotoRepository.countByDatingProfileId(PROFILE_ID)).thenReturn(3L);
            when(mediaUploadService.upload(mockFile, MediaUploadContext.PROFILE)).thenReturn(uploadResponse);
            when(datingProfilePhotoRepository.findTopByDatingProfileIdOrderByDisplayOrderDesc(PROFILE_ID))
                    .thenReturn(Optional.of(lastPhoto));

            datingProfilePhotoService.createPhoto(request);

            verify(datingProfilePhotoRepository).save(argThat(photo ->
                    Boolean.FALSE.equals(photo.getPrimary())
                            && photo.getDisplayOrder() == 3
            ));
        }
    }

    @Test
    void createPhoto_profileNotFound_throwsUserNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> datingProfilePhotoService.createPhoto(request));

            verify(datingProfilePhotoRepository, never()).save(any());
            verify(mediaUploadService, never()).upload(any(), any());
        }
    }

    @Test
    void createPhoto_limitExceeded_throwsDatingProfilePhotoLimitExceededException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(datingProfile));
            when(datingProfilePhotoRepository.countByDatingProfileId(PROFILE_ID)).thenReturn(8L);

            assertThrows(DatingProfilePhotoLimitExceededException.class,
                    () -> datingProfilePhotoService.createPhoto(request));

            verify(mediaUploadService, never()).upload(any(), any());
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }

    @Test
    void createPhoto_videoFile_throwsInvalidDatingProfilePhotoException() {
        UploadFileResponse videoResponse = UploadFileResponse.builder()
                .fileUrl("https://cdn.example.com/video.mp4")
                .publicId("DatingPhoto/vid123")
                .resourceType("VIDEO")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(datingProfile));
            when(datingProfilePhotoRepository.countByDatingProfileId(PROFILE_ID)).thenReturn(2L);
            when(mediaUploadService.upload(mockFile, MediaUploadContext.PROFILE)).thenReturn(videoResponse);

            assertThrows(InvalidDatingProfilePhotoException.class,
                    () -> datingProfilePhotoService.createPhoto(request));

            verify(mediaUploadService).upload(mockFile, MediaUploadContext.PROFILE);
            verify(mediaUploadService).delete("DatingPhoto/vid123", MediaType.VIDEO);
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // deletePhoto
    // ─────────────────────────────────────────────

    @Test
    void deletePhoto_primaryPhoto_promotesNextPhotoAsPrimary() {
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(datingProfile)
                .publicId("DatingPhoto/primary123")
                .displayOrder(0)
                .primary(true)
                .build();

        DatingProfilePhoto nextPhoto = DatingProfilePhoto.builder()
                .id(200L)
                .datingProfile(datingProfile)
                .displayOrder(1)
                .primary(false)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));
            when(datingProfilePhotoRepository.findTopByDatingProfileIdOrderByDisplayOrderAsc(PROFILE_ID))
                    .thenReturn(Optional.of(nextPhoto));

            datingProfilePhotoService.deletePhoto(PHOTO_ID);

            verify(mediaUploadService).delete("DatingPhoto/primary123", MediaType.IMAGE);
            verify(datingProfilePhotoRepository).delete(photo);
            verify(datingProfilePhotoRepository).decreaseDisplayOrderAfterDelete(PROFILE_ID, 0);
            assertTrue(nextPhoto.getPrimary());
            verify(datingProfilePhotoRepository).save(nextPhoto);
        }
    }

    @Test
    void deletePhoto_nonPrimaryPhoto_doesNotPromote() {
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(datingProfile)
                .publicId("DatingPhoto/secondary456")
                .displayOrder(2)
                .primary(false)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));

            datingProfilePhotoService.deletePhoto(PHOTO_ID);

            verify(mediaUploadService).delete("DatingPhoto/secondary456", MediaType.IMAGE);
            verify(datingProfilePhotoRepository).delete(photo);
            verify(datingProfilePhotoRepository).decreaseDisplayOrderAfterDelete(PROFILE_ID, 2);
            verify(datingProfilePhotoRepository, never()).findTopByDatingProfileIdOrderByDisplayOrderAsc(any());
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }

    @Test
    void deletePhoto_photoNotFound_throwsDatingProfilePhotoNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(DatingProfilePhotoNotFoundException.class,
                    () -> datingProfilePhotoService.deletePhoto(PHOTO_ID));

            verify(mediaUploadService, never()).delete(any(), any());
            verify(datingProfilePhotoRepository, never()).delete(any());
        }
    }

    @Test
    void deletePhoto_noPrimaryPromotion_whenNoNextPhoto() {
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(datingProfile)
                .publicId("DatingPhoto/onlyphoto")
                .displayOrder(0)
                .primary(true)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));
            when(datingProfilePhotoRepository.findTopByDatingProfileIdOrderByDisplayOrderAsc(PROFILE_ID))
                    .thenReturn(Optional.empty());

            datingProfilePhotoService.deletePhoto(PHOTO_ID);

            verify(mediaUploadService).delete("DatingPhoto/onlyphoto", MediaType.IMAGE);
            verify(datingProfilePhotoRepository).delete(photo);
            verify(datingProfilePhotoRepository).decreaseDisplayOrderAfterDelete(PROFILE_ID, 0);
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // updatePrimaryPhoto
    // ─────────────────────────────────────────────

    @Test
    void updatePrimaryPhoto_success_clearsOldPrimaryAndSetsNewOne() {
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(datingProfile)
                .primary(false)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));

            datingProfilePhotoService.updatePrimaryPhoto(PHOTO_ID);

            verify(datingProfilePhotoRepository).clearPrimaryPhoto(PROFILE_ID);
            assertTrue(photo.getPrimary());
            verify(datingProfilePhotoRepository).save(photo);
        }
    }

    @Test
    void updatePrimaryPhoto_photoNotFound_throwsDatingProfilePhotoNotFoundException() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(DatingProfilePhotoNotFoundException.class,
                    () -> datingProfilePhotoService.updatePrimaryPhoto(PHOTO_ID));

            verify(datingProfilePhotoRepository, never()).clearPrimaryPhoto(any());
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }

    @Test
    void updatePrimaryPhoto_alreadyPrimary_throwsDatingProfilePhotoAlreadyPrimaryException() {
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(datingProfile)
                .primary(true)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));

            assertThrows(DatingProfilePhotoAlreadyPrimaryException.class,
                    () -> datingProfilePhotoService.updatePrimaryPhoto(PHOTO_ID));

            verify(datingProfilePhotoRepository, never()).clearPrimaryPhoto(any());
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }

    @Test
    void deletePhoto_unauthorized_throwsUnauthorizedDatingProfilePhotoException() {
        User differentUser = User.builder().id(999L).build();
        DatingProfile otherProfile = DatingProfile.builder().id(20L).user(differentUser).build();
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(otherProfile)
                .publicId("DatingPhoto/other")
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));

            assertThrows(UnauthorizedDatingProfilePhotoException.class,
                    () -> datingProfilePhotoService.deletePhoto(PHOTO_ID));

            verify(datingProfilePhotoRepository, never()).delete(any());
        }
    }

    @Test
    void updatePrimaryPhoto_unauthorized_throwsUnauthorizedDatingProfilePhotoException() {
        User differentUser = User.builder().id(999L).build();
        DatingProfile otherProfile = DatingProfile.builder().id(20L).user(differentUser).build();
        DatingProfilePhoto photo = DatingProfilePhoto.builder()
                .id(PHOTO_ID)
                .datingProfile(otherProfile)
                .primary(false)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(USER_ID);

            when(datingProfilePhotoRepository.findByIdAndUserId(PHOTO_ID, USER_ID)).thenReturn(Optional.of(photo));

            assertThrows(UnauthorizedDatingProfilePhotoException.class,
                    () -> datingProfilePhotoService.updatePrimaryPhoto(PHOTO_ID));

            verify(datingProfilePhotoRepository, never()).clearPrimaryPhoto(any());
            verify(datingProfilePhotoRepository, never()).save(any());
        }
    }
}
