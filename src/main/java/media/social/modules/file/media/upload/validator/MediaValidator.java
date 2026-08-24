package media.social.modules.file.media.upload.validator;

import lombok.extern.slf4j.Slf4j;
import media.social.modules.file.exception.InvalidMediaException;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.post.enums.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Validates a file against a MediaUploadPolicy before upload.
 * Checks: null/empty, MIME type, extension, file size.
 * NOTE: Video duration is validated POST-upload in MediaUploadServiceImpl
 *       because duration is only available after Cloudinary processes the file.
 */
@Component
@Slf4j
public class MediaValidator {

    private static final List<String> IMAGE_MIME_PREFIXES = List.of("image/");
    private static final List<String> VIDEO_MIME_PREFIXES = List.of("video/");

    /**
     * Detects the MediaType from MIME type first, then falls back to extension.
     * Does NOT trust either source alone — uses both for safety.
     */
    public MediaType detectMediaType(MultipartFile file, MediaUploadPolicy policy) {

        validateNotEmpty(file);

        String contentType = file.getContentType();

        // Primary: MIME type detection
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return MediaType.IMAGE;
            }
            if (contentType.startsWith("video/")) {
                return MediaType.VIDEO;
            }
        }

        // Fallback: extension-based detection
        String extension = extractExtension(file.getOriginalFilename());

        if (policy.allowedImageExtensions().contains(extension)) {
            return MediaType.IMAGE;
        }

        if (policy.allowedVideoExtensions().contains(extension)) {
            return MediaType.VIDEO;
        }

        throw new InvalidMediaException("Unsupported media type. Cannot determine media type from file.");
    }

    /**
     * Validates a file against the given policy for the resolved media type.
     * Validates: null/empty, MIME type consistency, extension, file size.
     */
    public void validate(MultipartFile file, MediaType mediaType, MediaUploadPolicy policy) {

        validateNotEmpty(file);

        validateMimeType(file, mediaType);

        validateExtension(file.getOriginalFilename(), mediaType, policy);

        validateFileSize(file, mediaType, policy);
    }

    // ─── Duration (post-upload) ───────────────────────────────────────────────

    /**
     * Validates video duration against the policy's maximum.
     * Called after upload because Cloudinary returns duration in the upload result.
     *
     * @param actualDurationSeconds actual duration from Cloudinary upload result
     * @param context               human-readable context name for error messages (e.g. "Post", "Story")
     * @param policy                the policy to validate against
     */
    public void validateVideoDuration(
            long actualDurationSeconds,
            String context,
            MediaUploadPolicy policy
    ) {
        long maxDuration = policy.maxVideoDurationSeconds();

        if (maxDuration > 0 && actualDurationSeconds > maxDuration) {
            throw new InvalidMediaException(
                    context + " video duration must not exceed " + formatDuration(maxDuration)
            );
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("File must not be empty");
        }
    }

    private void validateMimeType(MultipartFile file, MediaType mediaType) {

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new InvalidMediaException("File content type is missing");
        }

        boolean validMime = switch (mediaType) {
            case IMAGE -> contentType.startsWith("image/");
            case VIDEO -> contentType.startsWith("video/");
        };

        if (!validMime) {
            throw new InvalidMediaException(
                    "Invalid content type for " + mediaType.name().toLowerCase() + ": " + contentType
            );
        }
    }

    private void validateExtension(String filename, MediaType mediaType, MediaUploadPolicy policy) {

        if (filename == null || !filename.contains(".")) {
            throw new InvalidMediaException("Invalid file name — cannot determine extension");
        }

        String extension = extractExtension(filename);

        List<String> allowedExtensions = switch (mediaType) {
            case IMAGE -> policy.allowedImageExtensions();
            case VIDEO -> policy.allowedVideoExtensions();
        };

        if (!allowedExtensions.contains(extension)) {
            throw new InvalidMediaException(
                    "Invalid " + mediaType.name().toLowerCase() +
                    " file extension: ." + extension +
                    ". Allowed: " + allowedExtensions
            );
        }
    }

    private void validateFileSize(MultipartFile file, MediaType mediaType, MediaUploadPolicy policy) {

        long maxSize = switch (mediaType) {
            case IMAGE -> policy.maxImageSizeBytes();
            case VIDEO -> policy.maxVideoSizeBytes();
        };

        if (file.getSize() > maxSize) {
            throw new InvalidMediaException(
                    mediaType.name().charAt(0) +
                    mediaType.name().substring(1).toLowerCase() +
                    " size exceeds " + formatSize(maxSize)
            );
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024 * 1024) {
            return (bytes / (1024 * 1024)) + "MB";
        }
        return (bytes / 1024) + "KB";
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        }
        return (seconds / 60) + " minutes";
    }
}
