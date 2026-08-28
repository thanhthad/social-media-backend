package media.social.modules.file.media.storage;

import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.post.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * Pure storage abstraction for Cloudinary.
 * Does NOT know about POST/STORY/MESSAGE/PROFILE.
 * Does NOT enforce business size/duration limits.
 * Responsible only for: upload to Cloudinary, delete from Cloudinary.
 */
public interface CloudinaryStorageService {

    /**
     * Uploads a file to Cloudinary.
     *
     * @param file      the file to upload
     * @param folder    target Cloudinary folder
     * @param mediaType IMAGE or VIDEO (maps to Cloudinary resource_type)
     * @return upload result with secureUrl, publicId, and available metadata
     */
    UploadFileResponse upload(MultipartFile file, String folder, MediaType mediaType);

    /**
     * Deletes a resource from Cloudinary.
     * If publicId is null or blank, the operation is silently skipped.
     *
     * @param publicId  Cloudinary public_id
     * @param mediaType used to determine Cloudinary resource_type
     */
    void delete(String publicId, MediaType mediaType);

    /**
     * Generates a video thumbnail / poster image URL at the specified offset in seconds.
     *
     * @param publicId      Cloudinary public_id of the video
     * @param offsetSeconds timestamp in seconds (e.g. 1.0)
     * @return direct secure URL to the derived frame image
     */
    String generateVideoThumbnailUrl(String publicId, double offsetSeconds);
}
