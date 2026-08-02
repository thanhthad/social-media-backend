package media.social.modules.user.service;

import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateProfileRequest;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.user.ProfileResponse;
import media.social.modules.user.dto.response.user.PublicUserProfileResponse;
import media.social.modules.user.dto.response.user.UserSearchResponse;
import media.social.modules.user.dto.response.user.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    public UserProfileResponse getMe();

    public ProfileResponse updateMe(UpdateProfileRequest request);

    public UserProfileResponse updateUserName(UpdateUsernameRequest request);

    public boolean hasUsername();

    public ProfileResponse updateAvatar(UpdateAvatarRequest request);

    public void updatePassword(ChangePasswordRequest request);

    public PublicUserProfileResponse getUserById(Long id);

    public Page<UserSearchResponse> findUsersByName(String username, Pageable pageable);

}
