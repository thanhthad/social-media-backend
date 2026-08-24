package media.social.modules.file.media.upload.policy;

import java.util.List;

/**
 * Describes the upload rules for a specific MediaUploadContext.
 * All business limits live here — not in CloudinaryStorageService or CloudinaryServiceImpl.
 *
 * @param maxImageSizeBytes         Maximum allowed image file size in bytes
 * @param maxVideoSizeBytes         Maximum allowed video file size in bytes
 * @param maxVideoDurationSeconds   Maximum allowed video duration in seconds (0 = no limit)
 * @param allowedImageExtensions    Allowed image file extensions (lowercase)
 * @param allowedVideoExtensions    Allowed video file extensions (lowercase)
 */
public record MediaUploadPolicy(
        long maxImageSizeBytes,
        long maxVideoSizeBytes,
        long maxVideoDurationSeconds,
        List<String> allowedImageExtensions,
        List<String> allowedVideoExtensions
) {
}
