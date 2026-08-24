package media.social.modules.file.media.upload;

/**
 * Represents the use-case context of a media upload.
 * Used by MediaUploadPolicyResolver to determine the appropriate MediaUploadPolicy.
 * Do NOT put business limits (maxSize, maxDuration) directly here.
 */
public enum MediaUploadContext {
    POST,
    STORY,
    MESSAGE,
    PROFILE
}
