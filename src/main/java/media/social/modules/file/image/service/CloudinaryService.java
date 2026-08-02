package media.social.modules.file.image.service;

import media.social.modules.file.image.dto.response.UploadFileResponse;
import media.social.modules.post.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    UploadFileResponse uploadFile(
            MultipartFile file,
            String folder,
            MediaType mediaType
    );

    void deleteFile(
            String publicId,
            MediaType mediaType
    );

    void validateFile(
            MultipartFile file,
            MediaType mediaType
    );
}