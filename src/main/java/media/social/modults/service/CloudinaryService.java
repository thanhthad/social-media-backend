package media.social.modults.service;

import media.social.modults.dto.response.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    UploadImageResponse uploadImage(MultipartFile file, String folder);

    void deleteImage(String publicId);
}