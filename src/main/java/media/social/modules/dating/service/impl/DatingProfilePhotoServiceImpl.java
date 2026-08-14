package media.social.modules.dating.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.profile.CreateDatingProfilePhotoRequest;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.entity.DatingProfilePhoto;
import media.social.modules.dating.exception.photo.*;
import media.social.modules.dating.repository.DatingProfilePhotoRepository;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.service.DatingProfilePhotoService;
import media.social.modules.file.image.dto.response.UploadFileResponse;
import media.social.modules.file.image.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.user.exception.user.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
public class DatingProfilePhotoServiceImpl implements DatingProfilePhotoService {

    private final DatingProfilePhotoRepository datingProfilePhotoRepository;
    private final CloudinaryService cloudinaryService;
    private final DatingProfileRepository datingProfileRepository;

    @Override
    @Transactional
    public void createPhoto(CreateDatingProfilePhotoRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile datingProfile =
                datingProfileRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Dating profile not found"
                                )
                        );

        long count =
                datingProfilePhotoRepository.countByDatingProfileId(
                        datingProfile.getId()
                );

        if (count >= 8) {
            throw new DatingProfilePhotoLimitExceededException(
                    "Dating profile cannot have more than 8 photos"
            );
        }

        MediaType type =
                cloudinaryService.detectMediaType(
                        request.getFile()
                );

        if (type.equals(MediaType.VIDEO)) {
            throw new InvalidDatingProfilePhotoException(
                    "Dating profile photo must be an image"
            );
        }

        cloudinaryService.validateFile(
                request.getFile(),
                type
        );

        UploadFileResponse result =
                cloudinaryService.uploadFile(
                        request.getFile(),
                        "DatingPhoto/",
                        type
                );

        Integer displayOrder =
                datingProfilePhotoRepository
                        .findTopByDatingProfileIdOrderByDisplayOrderDesc(
                                datingProfile.getId()
                        )
                        .map(photo ->
                                photo.getDisplayOrder() + 1
                        )
                        .orElse(0);

        DatingProfilePhoto datingProfilePhoto =
                DatingProfilePhoto.builder()
                        .datingProfile(datingProfile)
                        .imageUrl(result.getFileUrl())
                        .publicId(result.getPublicId())
                        .displayOrder(displayOrder)
                        .primary(count == 0)
                        .build();

        datingProfilePhotoRepository.save(
                datingProfilePhoto
        );
    }

    @Override
    @Transactional
    public void deletePhoto(Long photoId) {

        Long userId = UserContextHolder.getUserId();

        DatingProfilePhoto photo =
                datingProfilePhotoRepository
                        .findByIdAndUserId(photoId,userId)
                        .orElseThrow(() ->
                                new DatingProfilePhotoNotFoundException(
                                        "Dating profile photo not found"
                                )
                        );

        DatingProfile datingProfile =
                photo.getDatingProfile();

        if (!datingProfile.getUser().getId().equals(userId)) {
            throw new UnauthorizedDatingProfilePhotoException(
                    "You cannot delete this photo"
            );
        }

        boolean wasPrimary =
                Boolean.TRUE.equals(photo.getPrimary());

        Integer deletedOrder =
                photo.getDisplayOrder();

        cloudinaryService.deleteFile(
                photo.getPublicId(),
                MediaType.IMAGE
        );

        datingProfilePhotoRepository.delete(photo);

        datingProfilePhotoRepository
                .decreaseDisplayOrderAfterDelete(
                        datingProfile.getId(),
                        deletedOrder
                );

        if (wasPrimary) {

            datingProfilePhotoRepository
                    .findTopByDatingProfileIdOrderByDisplayOrderAsc(
                            datingProfile.getId()
                    )
                    .ifPresent(nextPrimary -> {

                        nextPrimary.setPrimary(true);

                        datingProfilePhotoRepository.save(
                                nextPrimary
                        );
                    });
        }
    }
    @Override
    @Transactional
    public void updatePrimaryPhoto(Long photoId) {

        Long userId = UserContextHolder.getUserId();

        DatingProfilePhoto photo =
                datingProfilePhotoRepository
                        .findByIdAndUserId(photoId,userId)
                        .orElseThrow(() ->
                                new DatingProfilePhotoNotFoundException(
                                        "Dating profile photo not found"
                                )
                        );

        DatingProfile datingProfile =
                photo.getDatingProfile();

        if (!datingProfile.getUser().getId().equals(userId)) {
            throw new UnauthorizedDatingProfilePhotoException(
                    "You cannot update this photo"
            );
        }

        if (Boolean.TRUE.equals(photo.getPrimary())) {
            throw new DatingProfilePhotoAlreadyPrimaryException(
                    "This photo is already primary"
            );
        }

        datingProfilePhotoRepository.clearPrimaryPhoto(
                datingProfile.getId()
        );

        photo.setPrimary(true);

        datingProfilePhotoRepository.save(photo);
    }
}
