package media.social.modules.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a file upload operation.
 * Extended with media metadata fields populated by CloudinaryStorageServiceImpl.
 * Existing callers only use fileUrl/publicId/resourceType — backward compatible.
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileResponse {

    /** Cloudinary secure_url */
    private String fileUrl;

    /** Cloudinary public_id */
    private String publicId;

    /** MediaType name: IMAGE or VIDEO */
    private String resourceType;

    // ─── Media Metadata ────────────────────────────────────────────────────────

    /** Video duration in seconds (0 if not a video or not available) */
    private long duration;

    /** Width in pixels (0 if not available) */
    private Integer width;

    /** Height in pixels (0 if not available) */
    private Integer height;

    /** File format as reported by Cloudinary (e.g. "mp4", "jpg") */
    private String format;

    /** File size in bytes */
    private long fileSize;
}