package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.dto.request.profile.CreateDatingProfilePhotoRequest;
import media.social.modules.dating.service.DatingProfilePhotoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating")
@RequiredArgsConstructor
@Tag(
        name = "Dating Profile Photo Controller",
        description = "Dating profile photo APIs"
)
public class DatingProfilePhotoController {

    private final DatingProfilePhotoService datingProfilePhotoService;

    @PostMapping(
            value = "/me/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Upload dating profile photo")
    @RateLimit(
            name = "DATING_UPLOAD_PROFILE_PHOTO",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> createPhoto(
            @Valid @ModelAttribute CreateDatingProfilePhotoRequest request
    ) {

        datingProfilePhotoService.createPhoto(request);

        return ResponseData.success(
                null,
                "Upload dating profile photo successfully",
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/me/photos/{photoId}")
    @Operation(summary = "Delete dating profile photo")
    @RateLimit(
            name = "DATING_DELETE_PROFILE_PHOTO",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> deletePhoto(
            @PathVariable Long photoId
    ) {

        datingProfilePhotoService.deletePhoto(photoId);

        return ResponseData.success(
                null,
                "Delete dating profile photo successfully",
                HttpStatus.OK
        );
    }

    @PatchMapping("/me/photos/{photoId}/primary")
    @Operation(summary = "Set dating profile photo as primary")
    @RateLimit(
            name = "DATING_UPDATE_PRIMARY_PHOTO",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> updatePrimaryPhoto(
            @PathVariable Long photoId
    ) {

        datingProfilePhotoService.updatePrimaryPhoto(photoId);

        return ResponseData.success(
                null,
                "Update primary dating profile photo successfully",
                HttpStatus.OK
        );
    }
}