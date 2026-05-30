package media.social.modults.file.image.service;

import media.social.modults.file.image.dto.response.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    UploadImageResponse uploadImage(MultipartFile file, String folder);

    void deleteImage(String publicId);

    void validateImage(MultipartFile file);
}