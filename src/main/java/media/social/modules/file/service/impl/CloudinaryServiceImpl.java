package media.social.modules.file.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.exception.CloudinaryDeleteException;
import media.social.modules.file.exception.CloudinaryUploadException;
import media.social.modules.file.exception.InvalidMediaException;
import media.social.modules.file.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;      // 5MB
    private static final long MAX_VIDEO_SIZE = 300  * 1024 * 1024;     // 300MB

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = List.of(
            "mp4",
            "mov",
            "avi",
            "mkv",
            "webm"
    );

    @Override
    public UploadFileResponse uploadFile(
            MultipartFile file,
            String folder,
            MediaType mediaType
    ) {
        validateFile(file, mediaType);

        File tempFile = null;

        try {

            String extension = getExtension(file.getOriginalFilename());

            tempFile = File.createTempFile(
                    "cloudinary-",
                    "." + extension
            );
            file.transferTo(tempFile);
            Map<String, Object> uploadResult =
                    cloudinary.uploader().upload(
                            tempFile,
                            ObjectUtils.asMap(
                                    "folder", folder,
                                    "resource_type", mediaType.name().toLowerCase()
                            )
                    );
            return new UploadFileResponse(
                    uploadResult.get("secure_url").toString(),
                    uploadResult.get("public_id").toString(),
                    mediaType.name()
            );
        } catch (IOException e) {
            log.error(
                    "Cloudinary upload failed | folder={}",
                    folder,
                    e
            );
            throw new CloudinaryUploadException(
                    "Failed to upload file to Cloudinary"
            );
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return "tmp";
        }

        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    @Override
    public void deleteFile(
            String publicId,
            MediaType mediaType
    ) {

        try {

            Map<String, Object> result =
                    cloudinary.uploader().destroy(
                            publicId,
                            ObjectUtils.asMap(
                                    "resource_type",
                                    mediaType.name().toLowerCase()
                            )
                    );

            log.info(
                    "Cloudinary delete success | type={} | publicId={} | result={}",
                    mediaType,
                    publicId,
                    result.get("result")
            );

        } catch (Exception e) {

            log.error(
                    "Cloudinary delete failed | publicId={}",
                    publicId,
                    e
            );

            throw new CloudinaryDeleteException(
                    "Failed to delete file from Cloudinary"
            );
        }
    }

    @Override
    public void validateFile(
            MultipartFile file,
            MediaType mediaType
    ) {

        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("File is empty");
        }

        long maxSize = mediaType == MediaType.IMAGE
                ? MAX_IMAGE_SIZE
                : MAX_VIDEO_SIZE;

        if (file.getSize() > maxSize) {
            throw new InvalidMediaException(
                    mediaType == MediaType.IMAGE
                            ? "Image size exceeds 5MB"
                            : "Video size exceeds 50MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new InvalidMediaException("Invalid content type");
        }

        if (mediaType == MediaType.IMAGE &&
                !contentType.startsWith("image/")) {

            throw new InvalidMediaException(
                    "Invalid image content type"
            );
        }

        if (mediaType == MediaType.VIDEO &&
                !contentType.startsWith("video/")) {

            throw new InvalidMediaException(
                    "Invalid video content type"
            );
        }

        validateExtension(
                file.getOriginalFilename(),
                mediaType
        );
    }

    @Override
    public MediaType detectMediaType(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("File is empty");
        }

        String contentType = file.getContentType();

        if (contentType != null) {

            if (contentType.startsWith("image/")) {
                return MediaType.IMAGE;
            }

            if (contentType.startsWith("video/")) {
                return MediaType.VIDEO;
            }
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.contains(".")) {
            throw new InvalidMediaException(
                    "Unable to determine media type"
            );
        }

        String extension = filename
                .substring(filename.lastIndexOf('.') + 1)
                .toLowerCase();

        if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return MediaType.IMAGE;
        }

        if (ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            return MediaType.VIDEO;
        }

        throw new InvalidMediaException(
                "Unsupported media type"
        );
    }

    private void validateExtension(
            String filename,
            MediaType mediaType
    ) {

        if (filename == null || !filename.contains(".")) {

            throw new InvalidMediaException(
                    "Invalid file name"
            );
        }

        String extension = filename.substring(
                filename.lastIndexOf(".") + 1
        ).toLowerCase();

        List<String> allowedExtensions =
                mediaType == MediaType.IMAGE
                        ? ALLOWED_IMAGE_EXTENSIONS
                        : ALLOWED_VIDEO_EXTENSIONS;

        if (!allowedExtensions.contains(extension)) {

            throw new InvalidMediaException(
                    "Invalid file extension. Allowed: " + allowedExtensions
            );
        }
    }
}