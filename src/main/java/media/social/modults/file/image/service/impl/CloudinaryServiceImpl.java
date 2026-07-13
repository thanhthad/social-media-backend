package media.social.modults.file.image.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modults.file.image.dto.response.UploadImageResponse;
import media.social.modults.file.image.exception.CloudinaryDeleteException;
import media.social.modults.file.image.exception.CloudinaryUploadException;
import media.social.modults.file.image.exception.InvalidImageException;
import media.social.modults.file.image.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    @Override
    public UploadImageResponse uploadImage(
            MultipartFile file,
            String folder
    ) {
        try {

            Map<String, Object> uploadResult =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder",
                                    folder
                            )
                    );

            String imageUrl =
                    uploadResult.get("secure_url").toString();

            String publicId =
                    uploadResult.get("public_id").toString();

            log.info(
                    "Cloudinary upload success | folder={} | publicId={}",
                    folder,
                    publicId
            );

            return UploadImageResponse.builder()
                    .imageUrl(imageUrl)
                    .publicId(publicId)
                    .build();

        } catch (IOException e) {

            log.error(
                    "Cloudinary upload failed | folder={}",
                    folder,
                    e
            );

            throw new CloudinaryUploadException(
                    "Failed to upload image to Cloudinary"
            );
        }
    }
    @Override
    public void deleteImage(String publicId) {

        try {

            Map<String, Object> result =
                    cloudinary.uploader().destroy(
                            publicId,
                            ObjectUtils.emptyMap()
                    );

            log.info(
                    "Cloudinary delete success | publicId={} | result={}",
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
                    "Failed to delete image from Cloudinary"
            );
        }
    }

    public void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidImageException(
                    "Image file is empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException(
                    "Image size exceeds 5MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new InvalidImageException(
                    "Invalid image content type"
            );
        }

        validateExtension(file.getOriginalFilename());
    }

    private void validateExtension(String filename) {

        if (filename == null || !filename.contains(".")) {

            throw new InvalidImageException(
                    "Invalid file name"
            );
        }

        String extension =
                filename.substring(
                                filename.lastIndexOf(".") + 1
                        )
                        .toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {

            throw new InvalidImageException(
                    "Invalid file extension. Allowed: "
                            + ALLOWED_EXTENSIONS
            );
        }
    }
}