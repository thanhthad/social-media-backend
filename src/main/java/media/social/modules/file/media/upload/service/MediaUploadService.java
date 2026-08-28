package media.social.modules.file.media.upload.service;

import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.post.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates the full media upload flow:
 * 1. Detect media type
 * 2. Resolve policy from context
 * 3. Validate file against policy
 * 4. Upload to storage
 * 5. Validate video duration (post-upload), cleanup on failure
 * 6. Return metadata response
 *
 * Module services (PostService, StoryService, etc.) should use this — not CloudinaryService directly.
 */
public interface MediaUploadService {

    /**
     * Uploads a file with policy validation appropriate for the given context.
     *
     * @param file    the file to upload
     * @param context the upload context (POST, STORY, MESSAGE, PROFILE)
     * @return upload result with URL, publicId and available metadata
     */
    UploadFileResponse upload(MultipartFile file, MediaUploadContext context);

    /**
     * Deletes a previously uploaded file from storage.
     * If publicId is null or blank, the operation is silently skipped.
     *
     * @param publicId  the storage public ID returned at upload time
     * @param mediaType IMAGE or VIDEO
     */
    void delete(String publicId, MediaType mediaType);

    /**
     * Generates a thumbnail image URL from an uploaded video at ~1s frame.
     *
     * @param publicId the storage public ID of the video
     * @return direct secure URL to the derived thumbnail image
     */
    String generateVideoThumbnailUrl(String publicId);
}
