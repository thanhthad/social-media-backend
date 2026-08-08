package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.profile.*;
import media.social.modules.dating.dto.response.profile.*;

public interface DatingProfileService {

    MyDatingProfileResponse getMe();

    DatingProfileResponse createProfile(
            CreateDatingProfileRequest request
    );

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
}