package media.social.modules.user.service;

import media.social.modules.user.dto.request.profile.*;
import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    /**
     * Cập nhật từng field riêng lẻ trên Profile (Facebook-style per-field edit).
     * Endpoint: PATCH /api/users/me/profile/field
     */
    ProfileResponse updateProfileField(UpdateProfileFieldRequest request);

    public MyProfileResponse getMe();

    ProfileResponse updateBasicProfile(
            UpdateBasicProfileRequest request
    );

    ProfileResponse updateContact(
            UpdateContactRequest request
    );

    ProfileResponse updateCareer(
            UpdateCareerRequest request
    );

    ProfileResponse updateSocialLinks(
            UpdateSocialLinksRequest request
    );

    ProfileResponse updateProfileVisibility(
            UpdateProfileVisibilityRequest request
    );

    public UserProfileResponse updateUserName(UpdateUsernameRequest request);

    public ProfileResponse updateCover(UpdateCoverRequest request);

    public ProfileResponse updateAvatar(UpdateAvatarRequest request);

    public void updatePassword(ChangePasswordRequest request);

    public PublicProfileResponse getUserById(Long id);

    public Page<UserSearchResponse> findUsersByName(String username, Pageable pageable);

    public UserStatsResponse getUserStats(Long targetUserId);
}
