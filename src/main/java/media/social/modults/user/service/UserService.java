package media.social.modults.user.service;

import media.social.modults.user.dto.request.user.UpdateAvatarRequest;
import media.social.modults.user.dto.request.user.ChangePasswordRequest;
import media.social.modults.user.dto.request.user.UpdateProfileRequest;
import media.social.modults.user.dto.response.user.PublicUserProfileResponse;
import media.social.modults.user.dto.response.user.UserSearchResponse;
import media.social.modults.user.dto.response.user.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    // USER SELF APIs (JWT / SecurityContext)
    public UserProfileResponse getMe();

    public UserProfileResponse updateMe(UpdateProfileRequest request);

    public UserProfileResponse updateAvatar(UpdateAvatarRequest request);

    public void updatePassword(ChangePasswordRequest request);

    // PUBLIC USER APIs
    public PublicUserProfileResponse getUserById(Long id);

    public Page<UserSearchResponse> findUsersByName(String username, Pageable pageable);

}
