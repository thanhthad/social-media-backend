package media.social.modules.file.media.upload.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.exception.InvalidMediaException;
import media.social.modules.file.media.storage.CloudinaryStorageService;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.file.media.upload.policy.MediaUploadPolicyResolver;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.file.media.upload.validator.MediaValidator;
import media.social.modules.post.enums.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates the full media upload pipeline:
 *   detectMediaType → resolvePolicy → validate → upload → validateDuration → return
 *
 * Video duration validation happens POST-upload because Cloudinary only reports
 * duration after processing. If duration validation fails:
 *   1. The uploaded resource is deleted from Cloudinary
 *   2. The original InvalidMediaException is thrown (not suppressed by cleanup failure)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUploadServiceImpl implements MediaUploadService {

    private final MediaUploadPolicyResolver policyResolver;
    private final MediaValidator mediaValidator;
    private final CloudinaryStorageService cloudinaryStorageService;

    @Override
    public UploadFileResponse upload(MultipartFile file, MediaUploadContext context) {

        // 1. Resolve policy for this context
        MediaUploadPolicy policy = policyResolver.resolve(context);

        // 2. Detect media type (MIME → extension fallback)
        MediaType mediaType = mediaValidator.detectMediaType(file, policy);

        // 3. Validate before upload (null/empty, MIME, extension, size)
        mediaValidator.validate(file, mediaType, policy);

        // 4. Upload to Cloudinary storage
        String folder = folderFor(context);

        UploadFileResponse uploadResult =
                cloudinaryStorageService.upload(file, folder, mediaType);

        // 5. Post-upload: validate video duration
        if (mediaType == MediaType.VIDEO) {
            validateDurationOrCleanup(uploadResult, context, policy);
        }

        return uploadResult;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Validates video duration against policy.
     * If duration exceeds limit: delete the uploaded resource, then throw.
     * If delete also fails: log the error but still throw the original validation exception.
     */
    private void validateDurationOrCleanup(
            UploadFileResponse uploadResult,
            MediaUploadContext context,
            MediaUploadPolicy policy
    ) {
        try {
            mediaValidator.validateVideoDuration(
                    uploadResult.getDuration(),
                    toContextLabel(context),
                    policy
            );
        } catch (InvalidMediaException validationException) {

            log.warn(
                    "Video duration validation failed after upload | context={} | publicId={} | duration={}s | limit={}s | Attempting cleanup...",
                    context,
                    uploadResult.getPublicId(),
                    uploadResult.getDuration(),
                    policy.maxVideoDurationSeconds()
            );

            try {
                cloudinaryStorageService.delete(
                        uploadResult.getPublicId(),
                        MediaType.VIDEO
                );
                log.info(
                        "Cleanup successful | publicId={} deleted after duration validation failure",
                        uploadResult.getPublicId()
                );
            } catch (Exception cleanupException) {
                log.error(
                        "Cleanup FAILED after duration validation failure | publicId={} | " +
                        "This resource is orphaned on Cloudinary and must be removed manually.",
                        uploadResult.getPublicId(),
                        cleanupException
                );
            }

            // Always re-throw the original business exception — not the cleanup failure
            throw validationException;
        }
    }

    /**
     * Determines the Cloudinary folder based on the upload context.
     * Storage folder structure is managed here, not in module services.
     */
    private String folderFor(MediaUploadContext context) {
        return switch (context) {
            case POST    -> "posts";
            case STORY   -> "stories";
            case MESSAGE -> "conversation/message";
            case PROFILE -> "profiles";
            case REEL ->  "reels";
        };
    }

    /**
     * Human-readable label for error messages.
     */
    private String toContextLabel(MediaUploadContext context) {
        return switch (context) {
            case POST    -> "Post";
            case STORY   -> "Story";
            case MESSAGE -> "Message";
            case PROFILE -> "Profile";
            case REEL ->  "reels";
        };
    }

    @Override
    public void delete(String publicId, MediaType mediaType) {
        cloudinaryStorageService.delete(publicId, mediaType);
    }

    @Override
    public String generateVideoThumbnailUrl(String publicId) {
        return cloudinaryStorageService.generateVideoThumbnailUrl(publicId, 1.0);
    }
}
