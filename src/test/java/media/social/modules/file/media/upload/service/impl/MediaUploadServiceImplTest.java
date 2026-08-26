package media.social.modules.file.media.upload.service.impl;

import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.exception.CloudinaryUploadException;
import media.social.modules.file.exception.InvalidMediaException;
import media.social.modules.file.media.storage.CloudinaryStorageService;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.file.media.upload.policy.MediaUploadPolicyResolver;
import media.social.modules.file.media.upload.validator.MediaValidator;
import media.social.modules.post.enums.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaUploadServiceImpl")
class MediaUploadServiceImplTest {

    @Mock private MediaUploadPolicyResolver policyResolver;
    @Mock private MediaValidator mediaValidator;
    @Mock private CloudinaryStorageService cloudinaryStorageService;

    @InjectMocks
    private MediaUploadServiceImpl service;

    private MediaUploadPolicy postPolicy;
    private MockMultipartFile imageFile;
    private MockMultipartFile videoFile;

    @BeforeEach
    void setUp() {
        postPolicy = new MediaUploadPolicy(
                5 * 1024 * 1024L,
                300L * 1024 * 1024,
                600L,
                List.of("jpg", "jpeg", "png", "webp"),
                List.of("mp4", "mov", "avi", "mkv", "webm")
        );

        imageFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[1024]
        );

        videoFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[1024]
        );
    }

    @Test
    @DisplayName("Successful image upload returns response with correct metadata")
    void upload_successfulImageUpload_returnsResponse() {
        when(policyResolver.resolve(MediaUploadContext.POST)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(imageFile, postPolicy)).thenReturn(MediaType.IMAGE);

        UploadFileResponse expectedResponse = UploadFileResponse.builder()
                .fileUrl("https://res.cloudinary.com/test/image/upload/posts/photo.jpg")
                .publicId("posts/abc123")
                .resourceType("IMAGE")
                .width(800)
                .height(600)
                .format("jpg")
                .fileSize(1024L)
                .build();

        when(cloudinaryStorageService.upload(imageFile, "posts", MediaType.IMAGE))
                .thenReturn(expectedResponse);

        UploadFileResponse result = service.upload(imageFile, MediaUploadContext.POST);

        assertThat(result.getFileUrl()).isEqualTo(expectedResponse.getFileUrl());
        assertThat(result.getPublicId()).isEqualTo(expectedResponse.getPublicId());
        assertThat(result.getResourceType()).isEqualTo("IMAGE");
        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(600);

        verify(mediaValidator).validate(imageFile, MediaType.IMAGE, postPolicy);
        verify(cloudinaryStorageService).upload(imageFile, "posts", MediaType.IMAGE);
        // No duration check for images
        verify(mediaValidator, never()).validateVideoDuration(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("Successful video upload validates duration and returns response")
    void upload_successfulVideoUpload_validatesDuration() {
        when(policyResolver.resolve(MediaUploadContext.POST)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(videoFile, postPolicy)).thenReturn(MediaType.VIDEO);

        UploadFileResponse expectedResponse = UploadFileResponse.builder()
                .fileUrl("https://res.cloudinary.com/test/video/upload/posts/video.mp4")
                .publicId("posts/xyz789")
                .resourceType("VIDEO")
                .duration(120L)
                .format("mp4")
                .fileSize(1024L)
                .build();

        when(cloudinaryStorageService.upload(videoFile, "posts", MediaType.VIDEO))
                .thenReturn(expectedResponse);

        UploadFileResponse result = service.upload(videoFile, MediaUploadContext.POST);

        assertThat(result.getDuration()).isEqualTo(120L);

        verify(mediaValidator).validateVideoDuration(120L, "Post", postPolicy);
    }

    @Test
    @DisplayName("Cloudinary upload failure throws CloudinaryUploadException")
    void upload_cloudinaryFailure_throwsException() {
        when(policyResolver.resolve(MediaUploadContext.POST)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(imageFile, postPolicy)).thenReturn(MediaType.IMAGE);
        when(cloudinaryStorageService.upload(any(), anyString(), any()))
                .thenThrow(new CloudinaryUploadException("Cloudinary error"));

        assertThatThrownBy(() -> service.upload(imageFile, MediaUploadContext.POST))
                .isInstanceOf(CloudinaryUploadException.class);
    }

    @Test
    @DisplayName("Duration validation failure triggers cleanup and re-throws InvalidMediaException")
    void upload_durationValidationFails_cleanupAndRethrow() {
        when(policyResolver.resolve(MediaUploadContext.STORY)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(videoFile, postPolicy)).thenReturn(MediaType.VIDEO);

        UploadFileResponse uploadedResponse = UploadFileResponse.builder()
                .fileUrl("https://res.cloudinary.com/test/video/upload/stories/video.mp4")
                .publicId("stories/orphan123")
                .resourceType("VIDEO")
                .duration(60L) // exceeds 30s story limit
                .build();

        when(cloudinaryStorageService.upload(videoFile, "stories", MediaType.VIDEO))
                .thenReturn(uploadedResponse);

        doThrow(new InvalidMediaException("Story video duration must not exceed 30 seconds"))
                .when(mediaValidator).validateVideoDuration(60L, "Story", postPolicy);

        assertThatThrownBy(() -> service.upload(videoFile, MediaUploadContext.STORY))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("30 seconds");

        // Verify cleanup was attempted
        verify(cloudinaryStorageService).delete("stories/orphan123", MediaType.VIDEO);
    }

    @Test
    @DisplayName("Cleanup failure after duration validation does not suppress original exception")
    void upload_cleanupFails_stillThrowsOriginalException() {
        when(policyResolver.resolve(MediaUploadContext.STORY)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(videoFile, postPolicy)).thenReturn(MediaType.VIDEO);

        UploadFileResponse uploadedResponse = UploadFileResponse.builder()
                .publicId("stories/orphan456")
                .resourceType("VIDEO")
                .duration(90L)
                .build();

        when(cloudinaryStorageService.upload(videoFile, "stories", MediaType.VIDEO))
                .thenReturn(uploadedResponse);

        InvalidMediaException originalException =
                new InvalidMediaException("Story video duration must not exceed 30 seconds");

        doThrow(originalException)
                .when(mediaValidator).validateVideoDuration(90L, "Story", postPolicy);

        doThrow(new RuntimeException("Cloudinary delete network error"))
                .when(cloudinaryStorageService).delete("stories/orphan456", MediaType.VIDEO);

        // Should still throw the original validation exception, not the cleanup exception
        assertThatThrownBy(() -> service.upload(videoFile, MediaUploadContext.STORY))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("30 seconds");
    }

    @Test
    @DisplayName("STORY uploads to 'stories' folder")
    void upload_story_usesStoriesFolder() {
        when(policyResolver.resolve(MediaUploadContext.STORY)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(imageFile, postPolicy)).thenReturn(MediaType.IMAGE);
        when(cloudinaryStorageService.upload(any(), eq("stories"), any()))
                .thenReturn(UploadFileResponse.builder()
                        .fileUrl("url")
                        .publicId("pid")
                        .resourceType("IMAGE")
                        .build());

        service.upload(imageFile, MediaUploadContext.STORY);

        verify(cloudinaryStorageService).upload(imageFile, "stories", MediaType.IMAGE);
    }

    @Test
    @DisplayName("MESSAGE uploads to 'conversation/message' folder")
    void upload_message_usesMessageFolder() {
        when(policyResolver.resolve(MediaUploadContext.MESSAGE)).thenReturn(postPolicy);
        when(mediaValidator.detectMediaType(imageFile, postPolicy)).thenReturn(MediaType.IMAGE);
        when(cloudinaryStorageService.upload(any(), eq("conversation/message"), any()))
                .thenReturn(UploadFileResponse.builder()
                        .fileUrl("url")
                        .publicId("pid")
                        .resourceType("IMAGE")
                        .build());

        service.upload(imageFile, MediaUploadContext.MESSAGE);

        verify(cloudinaryStorageService).upload(imageFile, "conversation/message", MediaType.IMAGE);
    }

    @Test
    @DisplayName("delete delegates to cloudinaryStorageService.delete")
    void delete_delegatesToStorageService() {
        service.delete("test-pub-id", MediaType.IMAGE);

        verify(cloudinaryStorageService).delete("test-pub-id", MediaType.IMAGE);
    }
}
