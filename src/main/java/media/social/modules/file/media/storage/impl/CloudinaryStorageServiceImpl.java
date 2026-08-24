package media.social.modules.file.media.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.exception.CloudinaryDeleteException;
import media.social.modules.file.exception.CloudinaryUploadException;
import media.social.modules.file.media.storage.CloudinaryStorageService;
import media.social.modules.post.enums.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary storage implementation.
 * Handles upload and delete operations against the Cloudinary API.
 * All Cloudinary-specific details (resource_type mapping, temp file handling) are encapsulated here.
 * Business rules (size limits, duration limits) must NOT be added here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageServiceImpl implements CloudinaryStorageService {

    private final Cloudinary cloudinary;

    /**
     * Explicit mapping from domain MediaType to Cloudinary resource_type string.
     * Storage layer owns this mapping — business layer does not need to know.
     */
    private String toCloudinaryResourceType(MediaType mediaType) {
        return switch (mediaType) {
            case IMAGE -> "image";
            case VIDEO -> "video";
        };
    }

    @Override
    public UploadFileResponse upload(MultipartFile file, String folder, MediaType mediaType) {

        String resourceType = toCloudinaryResourceType(mediaType);

        File tempFile = null;

        try {
            String extension = getExtension(file.getOriginalFilename());

            tempFile = File.createTempFile("cloudinary-upload-", "." + extension);

            file.transferTo(tempFile);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    tempFile,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType
                    )
            );

            return mapToResponse(uploadResult, mediaType);

        } catch (IOException e) {
            log.error("Cloudinary upload failed | folder={} | mediaType={}", folder, mediaType, e);
            throw new CloudinaryUploadException("Failed to upload file to Cloudinary");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public void delete(String publicId, MediaType mediaType) {

        if (publicId == null || publicId.isBlank()) {
            log.info("Cloudinary delete skipped | publicId is null or blank");
            return;
        }

        String resourceType = toCloudinaryResourceType(mediaType);

        try {
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );

            log.info(
                    "Cloudinary delete success | mediaType={} | publicId={} | result={}",
                    mediaType,
                    publicId,
                    result.get("result")
            );

        } catch (Exception e) {
            log.error("Cloudinary delete failed | publicId={} | mediaType={}", publicId, mediaType, e);
            throw new CloudinaryDeleteException("Failed to delete file from Cloudinary: " + publicId);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UploadFileResponse mapToResponse(Map<String, Object> result, MediaType mediaType) {

        String secureUrl = getString(result, "secure_url");
        String publicId  = getString(result, "public_id");
        String format    = getString(result, "format");

        Double durationRaw = getDouble(result, "duration");
        long duration      = durationRaw != null ? durationRaw.longValue() : 0L;

        Integer width  = getInteger(result, "width");
        Integer height = getInteger(result, "height");

        Object fileSizeRaw = result.get("bytes");
        long fileSize = fileSizeRaw instanceof Number n ? n.longValue() : 0L;

        return UploadFileResponse.builder()
                .fileUrl(secureUrl)
                .publicId(publicId)
                .resourceType(mediaType.name())
                .duration(duration)
                .width(width)
                .height(height)
                .format(format)
                .fileSize(fileSize)
                .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "tmp";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
