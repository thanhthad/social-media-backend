package media.social.modules.file.media.upload.validator;

import media.social.modules.file.exception.InvalidMediaException;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.post.enums.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MediaValidator")
class MediaValidatorTest {

    private MediaValidator validator;

    private MediaUploadPolicy postPolicy;
    private MediaUploadPolicy storyPolicy;
    private MediaUploadPolicy messagePolicy;

    @BeforeEach
    void setUp() {
        validator = new MediaValidator();

        List<String> imageExts = List.of("jpg", "jpeg", "png", "webp");
        List<String> videoExts = List.of("mp4", "mov", "avi", "mkv", "webm");
        long imageSizeLimit = 5 * 1024 * 1024L; // 5MB

        postPolicy = new MediaUploadPolicy(
                imageSizeLimit,
                300L * 1024 * 1024,  // 300MB video
                600L,                 // 10 minutes
                imageExts,
                videoExts
        );

        storyPolicy = new MediaUploadPolicy(
                imageSizeLimit,
                100L * 1024 * 1024,  // 100MB video
                30L,                  // 30 seconds
                imageExts,
                videoExts
        );

        messagePolicy = new MediaUploadPolicy(
                imageSizeLimit,
                50L * 1024 * 1024,   // 50MB video
                300L,                 // 5 minutes
                imageExts,
                videoExts
        );
    }

    // ─── detectMediaType ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty file throws InvalidMediaException on detectMediaType")
    void detectMediaType_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> validator.detectMediaType(file, postPolicy))
                .isInstanceOf(InvalidMediaException.class);
    }

    @Test
    @DisplayName("Image MIME type detected correctly")
    void detectMediaType_imageMime_returnsImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[100]
        );

        assertThat(validator.detectMediaType(file, postPolicy)).isEqualTo(MediaType.IMAGE);
    }

    @Test
    @DisplayName("Video MIME type detected correctly")
    void detectMediaType_videoMime_returnsVideo() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[100]
        );

        assertThat(validator.detectMediaType(file, postPolicy)).isEqualTo(MediaType.VIDEO);
    }

    @Test
    @DisplayName("Falls back to extension when MIME is null — image extension")
    void detectMediaType_nullMimeImageExt_returnsImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", null, new byte[100]
        );

        assertThat(validator.detectMediaType(file, postPolicy)).isEqualTo(MediaType.IMAGE);
    }

    @Test
    @DisplayName("Falls back to extension when MIME is null — video extension")
    void detectMediaType_nullMimeVideoExt_returnsVideo() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", null, new byte[100]
        );

        assertThat(validator.detectMediaType(file, postPolicy)).isEqualTo(MediaType.VIDEO);
    }

    // ─── validate ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Null file throws InvalidMediaException")
    void validate_nullFile_throwsException() {
        assertThatThrownBy(() -> validator.validate(null, MediaType.IMAGE, postPolicy))
                .isInstanceOf(InvalidMediaException.class);
    }

    @Test
    @DisplayName("Empty file throws InvalidMediaException")
    void validate_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file, MediaType.IMAGE, postPolicy))
                .isInstanceOf(InvalidMediaException.class);
    }

    @Test
    @DisplayName("Valid image passes validation")
    void validate_validImage_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[1024]
        );

        // Should not throw
        validator.validate(file, MediaType.IMAGE, postPolicy);
    }

    @Test
    @DisplayName("Image with invalid MIME type throws InvalidMediaException")
    void validate_invalidImageMime_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "video/mp4", new byte[1024]
        );

        assertThatThrownBy(() -> validator.validate(file, MediaType.IMAGE, postPolicy))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("content type");
    }

    @Test
    @DisplayName("Image with invalid extension throws InvalidMediaException")
    void validate_invalidImageExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.bmp", "image/bmp", new byte[1024]
        );

        assertThatThrownBy(() -> validator.validate(file, MediaType.IMAGE, postPolicy))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("extension");
    }

    @Test
    @DisplayName("Image exceeding 5MB throws InvalidMediaException")
    void validate_imageExceeds5MB_throwsException() {
        byte[] bigFile = new byte[6 * 1024 * 1024]; // 6MB

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", bigFile
        );

        assertThatThrownBy(() -> validator.validate(file, MediaType.IMAGE, postPolicy))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    @DisplayName("Valid video for POST passes validation")
    void validate_validPostVideo_passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[1024]
        );

        // Should not throw
        validator.validate(file, MediaType.VIDEO, postPolicy);
    }

    @Test
    @DisplayName("Video exceeding POST limit (300MB) throws InvalidMediaException")
    void validate_postVideoExceeds300MB_throwsException() {
        // Create a mock file that reports a large size
        byte[] content = new byte[10]; // actual bytes small but we'll mock size
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[(int)(301L * 1024 * 1024 > Integer.MAX_VALUE ? Integer.MAX_VALUE : 301 * 1024)]
        ) {
            @Override
            public long getSize() {
                return 301L * 1024 * 1024; // 301MB
            }
        };

        assertThatThrownBy(() -> validator.validate(file, MediaType.VIDEO, postPolicy))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("300MB");
    }

    @Test
    @DisplayName("Video exceeding STORY limit (100MB) throws InvalidMediaException")
    void validate_storyVideoExceeds100MB_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[100]
        ) {
            @Override
            public long getSize() {
                return 101L * 1024 * 1024; // 101MB
            }
        };

        assertThatThrownBy(() -> validator.validate(file, MediaType.VIDEO, storyPolicy))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("100MB");
    }

    @Test
    @DisplayName("Video exceeding MESSAGE limit (50MB) throws InvalidMediaException")
    void validate_messageVideoExceeds50MB_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[100]
        ) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024; // 51MB
            }
        };

        assertThatThrownBy(() -> validator.validate(file, MediaType.VIDEO, messagePolicy))
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("50MB");
    }

    // ─── validateVideoDuration ────────────────────────────────────────────────

    @Test
    @DisplayName("POST video exceeding 10 minutes throws InvalidMediaException")
    void validateDuration_postVideoExceeds10Minutes_throwsException() {
        assertThatThrownBy(() ->
                validator.validateVideoDuration(601L, "Post", postPolicy)
        )
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Post")
                .hasMessageContaining("10 minutes");
    }

    @Test
    @DisplayName("STORY video exceeding 30 seconds throws InvalidMediaException")
    void validateDuration_storyVideoExceeds30Seconds_throwsException() {
        assertThatThrownBy(() ->
                validator.validateVideoDuration(31L, "Story", storyPolicy)
        )
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Story")
                .hasMessageContaining("30 seconds");
    }

    @Test
    @DisplayName("MESSAGE video exceeding 5 minutes throws InvalidMediaException")
    void validateDuration_messageVideoExceeds5Minutes_throwsException() {
        assertThatThrownBy(() ->
                validator.validateVideoDuration(301L, "Message", messagePolicy)
        )
                .isInstanceOf(InvalidMediaException.class)
                .hasMessageContaining("Message")
                .hasMessageContaining("5 minutes");
    }

    @Test
    @DisplayName("Video within POST duration limit passes")
    void validateDuration_postVideoWithinLimit_passes() {
        // 600 seconds = exactly at limit — should pass
        validator.validateVideoDuration(600L, "Post", postPolicy);
    }

    @Test
    @DisplayName("Zero maxDuration means no limit — always passes")
    void validateDuration_zeroDurationLimit_alwaysPasses() {
        MediaUploadPolicy noLimitPolicy = new MediaUploadPolicy(
                5 * 1024 * 1024L, 300L * 1024 * 1024, 0L,
                List.of("jpg"), List.of("mp4")
        );

        // Even 1 hour should pass
        validator.validateVideoDuration(3600L, "Profile", noLimitPolicy);
    }
}
