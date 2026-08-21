package media.social.modules.file.service;

import media.social.modules.file.dto.response.UploadFileResponse;
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

    MediaType detectMediaType(MultipartFile file);
}