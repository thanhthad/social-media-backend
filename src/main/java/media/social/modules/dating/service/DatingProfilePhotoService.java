package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.profile.CreateDatingProfilePhotoRequest;

public interface DatingProfilePhotoService {

    void createPhoto(
            CreateDatingProfilePhotoRequest request
    );

    void deletePhoto(
            Long photoId
    );

    void updatePrimaryPhoto(
            Long photoId
    );
}