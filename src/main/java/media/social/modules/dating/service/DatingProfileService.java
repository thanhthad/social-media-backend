package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.profile.*;
import media.social.modules.dating.dto.response.profile.*;

public interface DatingProfileService {

    MyDatingProfileResponse getMe();

    DatingProfileResponse updateBasicInfo(
            UpdateDatingBasicInfoRequest request
    );

    DatingProfileResponse updateCareer(
            UpdateDatingCareerRequest request
    );

    DatingProfileResponse updateLocation(
            UpdateDatingLocationRequest request
    );

    DatingProfileResponse updateCoordinates(
            UpdateDatingCoordinatesRequest request
    );

    DatingProfileResponse updateBio(
            UpdateDatingBioRequest request
    );

    DatingProfileResponse updateVisibility(
            UpdateDatingVisibilityRequest request
    );

    DatingProfileResponse updateStatus(
            UpdateDatingStatusRequest request
    );

    PublicDatingProfileResponse getPublicProfile(
            Long userId
    );

    void deleteProfile();

    /**
     * Cập nhật từng field riêng lẻ trên hồ sơ dating (Facebook-style inline edit).
     * Endpoint: PATCH /api/dating/me/profile/field
     */
    DatingProfileResponse updateDatingProfileField(UpdateDatingProfileFieldRequest request);
}