package media.social.modules.file.media.upload.policy;

import media.social.modules.file.media.upload.MediaUploadContext;

/**
 * Resolves the appropriate MediaUploadPolicy for a given MediaUploadContext.
 * Business rules (file size, duration limits) are encapsulated in the policy — not in storage or callers.
 */
public interface MediaUploadPolicyResolver {

    MediaUploadPolicy resolve(MediaUploadContext context);
}
