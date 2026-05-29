package media.social.modults.user.service;

import media.social.modults.user.dto.request.UpdateProfileRequest;
import media.social.modults.user.dto.response.UserProfileResponse;

public interface UserService {

    public UserProfileResponse getMe();

    public UserProfileResponse updateMe(UpdateProfileRequest request);

}
