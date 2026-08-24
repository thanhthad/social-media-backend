package media.social.modules.file.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.storage.CloudinaryStorageService;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.file.media.upload.policy.MediaUploadPolicyResolver;
import media.social.modules.file.media.upload.validator.MediaValidator;
import media.social.modules.file.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Backward-compatible adapter that implements the legacy CloudinaryService interface.
 * Delegates all operations to the new layered architecture:
 *   - validateFile/detectMediaType → MediaValidator (with PROFILE policy as default)
 *   - uploadFile                   → CloudinaryStorageService
 *   - deleteFile                   → CloudinaryStorageService
 *
 * Existing callers (ConversationServiceImpl, UserServiceImpl, DatingProfilePhotoServiceImpl)
 * continue to work without modification.
 * New callers should use MediaUploadService directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final CloudinaryStorageService cloudinaryStorageService;
    private final MediaValidator mediaValidator;
    private final MediaUploadPolicyResolver policyResolver;

    @Override
    public UploadFileResponse uploadFile(
            MultipartFile file,
            String folder,
            MediaType mediaType
    ) {
        return cloudinaryStorageService.upload(file, folder, mediaType);
    }

    @Override
    public void deleteFile(
            String publicId,
            MediaType mediaType
    ) {
        cloudinaryStorageService.delete(publicId, mediaType);
    }

    @Override
    public void validateFile(
            MultipartFile file,
            MediaType mediaType
    ) {
        // Use PROFILE policy as the default for legacy callers (image-only, 5MB limit)
        MediaUploadPolicy policy = policyResolver.resolve(MediaUploadContext.PROFILE);
        mediaValidator.validate(file, mediaType, policy);
    }

    @Override
    public MediaType detectMediaType(MultipartFile file) {
        // Use PROFILE policy for type detection (covers all allowed extensions)
        MediaUploadPolicy policy = policyResolver.resolve(MediaUploadContext.PROFILE);
        return mediaValidator.detectMediaType(file, policy);
    }
}