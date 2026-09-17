package media.social.modules.file.media.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.exception.CloudinaryDeleteException;
import media.social.modules.file.exception.CloudinaryUploadException;
import media.social.modules.post.enums.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CloudinaryStorageServiceImpl")
class CloudinaryStorageServiceImplTest {

    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    @InjectMocks
    private CloudinaryStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    // ─── upload ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Upload success returns response with all metadata")
    void upload_success_returnsResponseWithMetadata() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[100]
        );

        Map<String, Object> cloudinaryResult = new HashMap<>();
        cloudinaryResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/posts/photo.jpg");
        cloudinaryResult.put("public_id", "posts/abc123");
        cloudinaryResult.put("format", "jpg");
        cloudinaryResult.put("width", 800);
        cloudinaryResult.put("height", 600);
        cloudinaryResult.put("bytes", 1024L);
        cloudinaryResult.put("duration", null);

        when(uploader.upload(any(File.class), anyMap())).thenReturn(cloudinaryResult);

        UploadFileResponse response = service.upload(file, "posts", MediaType.IMAGE);

        assertThat(response.getFileUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/posts/photo.jpg");
        assertThat(response.getPublicId()).isEqualTo("posts/abc123");
        assertThat(response.getResourceType()).isEqualTo("IMAGE");
        assertThat(response.getFormat()).isEqualTo("jpg");
        assertThat(response.getWidth()).isEqualTo(800);
        assertThat(response.getHeight()).isEqualTo(600);
        assertThat(response.getFileSize()).isEqualTo(1024L);
        assertThat(response.getDuration()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Video upload includes duration in response")
    void upload_video_includesDuration() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[100]
        );

        Map<String, Object> cloudinaryResult = new HashMap<>();
        cloudinaryResult.put("secure_url", "https://res.cloudinary.com/demo/video/upload/posts/video.mp4");
        cloudinaryResult.put("public_id", "posts/vid123");
        cloudinaryResult.put("format", "mp4");
        cloudinaryResult.put("width", 1920);
        cloudinaryResult.put("height", 1080);
        cloudinaryResult.put("bytes", 5000000L);
        cloudinaryResult.put("duration", 120.5);

        when(uploader.upload(any(File.class), anyMap())).thenReturn(cloudinaryResult);

        UploadFileResponse response = service.upload(file, "posts", MediaType.VIDEO);

        assertThat(response.getDuration()).isEqualTo(120L); // truncated to seconds
        assertThat(response.getResourceType()).isEqualTo("VIDEO");
    }

    @Test
    @DisplayName("Upload failure throws CloudinaryUploadException")
    void upload_ioException_throwsCloudinaryUploadException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[100]
        );

        when(uploader.upload(any(File.class), anyMap()))
                .thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> service.upload(file, "posts", MediaType.IMAGE))
                .isInstanceOf(CloudinaryUploadException.class)
                .hasMessageContaining("Failed to upload");
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Delete success calls Cloudinary destroy")
    void delete_success_callsCloudinaryDestroy() throws IOException {
        Map<String, Object> destroyResult = Map.of("result", "ok");
        when(uploader.destroy(anyString(), anyMap())).thenReturn(destroyResult);

        service.delete("posts/abc123", MediaType.IMAGE);

        verify(uploader).destroy(eq("posts/abc123"), anyMap());
    }

    @Test
    @DisplayName("Delete with null publicId is skipped silently")
    void delete_nullPublicId_skipped() {
        service.delete(null, MediaType.IMAGE);

        verifyNoInteractions(uploader);
    }

    @Test
    @DisplayName("Delete with blank publicId is skipped silently")
    void delete_blankPublicId_skipped() {
        service.delete("   ", MediaType.IMAGE);

        verifyNoInteractions(uploader);
    }

    @Test
    @DisplayName("Delete failure throws CloudinaryDeleteException")
    void delete_exception_throwsCloudinaryDeleteException() throws IOException {
        when(uploader.destroy(anyString(), anyMap()))
                .thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> service.delete("posts/abc123", MediaType.IMAGE))
                .isInstanceOf(CloudinaryDeleteException.class)
                .hasMessageContaining("Failed to delete");
    }
}
