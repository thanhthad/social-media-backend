package media.social.modults.user.service;

import media.social.modults.user.dto.request.self.UpdateAvatarRequest;
import media.social.modults.user.dto.request.self.ChangePasswordRequest;
import media.social.modults.user.dto.request.self.UpdateProfileRequest;
import media.social.modults.user.dto.response.pub.PublicUserProfileResponse;
import media.social.modults.user.dto.response.pub.UserSearchResponse;
import media.social.modults.user.dto.response.self.UserProfileResponse;
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
