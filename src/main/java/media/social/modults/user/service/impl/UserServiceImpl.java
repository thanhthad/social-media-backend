package media.social.modults.user.service.impl;
import media.social.modults.post.enums.MediaType;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import media.social.modults.file.image.dto.response.UploadFileResponse;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.user.Enum.Status;
import media.social.modults.user.dto.request.user.ChangePasswordRequest;
import media.social.modults.user.dto.request.user.UpdateAvatarRequest;
import media.social.modults.user.dto.request.user.UpdateProfileRequest;
import media.social.modults.user.dto.response.user.PublicUserProfileResponse;
import media.social.modults.user.dto.response.user.UserSearchResponse;
import media.social.modults.user.dto.response.user.UserProfileResponse;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.profile.ProfileNotFoundException;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.mapper.ProfileMapper;
import media.social.modults.user.mapper.UserMapper;
import media.social.modults.user.repository.ProfileRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        Long userId = UserContextHolder.getUserId();

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found  "
                        )
                );
    }

    private Profile getCurrentProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ProfileNotFoundException(
                                "Profile not found "
                        )
                );
    }

    private PublicUserProfileResponse buildPublicProfileResponse(
            User user,
            Profile profile
    ) {

        return PublicUserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .location(profile.getLocation())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMe() {

        User user = getCurrentUser();
        Profile profile = getCurrentProfile(user.getId());

        return userMapper.toUserProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMe(UpdateProfileRequest request) {

        User user = getCurrentUser();
        Profile profile = getCurrentProfile(user.getId());

        profileMapper.updateProfileFromRequest(request, profile);

        profileRepository.save(profile);

        return userMapper.toUserProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateAvatar(UpdateAvatarRequest request) {

        User user = getCurrentUser();
        Profile profile = getCurrentProfile(user.getId());

        cloudinaryService.validateFile(
                request.getFile(),
                MediaType.IMAGE
        );

        UploadFileResponse upload =
                cloudinaryService.uploadFile(
                        request.getFile(),
                        "avatars",
                        MediaType.IMAGE
                );

        if (profile.getAvatarPublicId() != null) {
            cloudinaryService.deleteFile(
                    profile.getAvatarPublicId(),
                    MediaType.IMAGE
            );
        }

        profile.setAvatarUrl(upload.getFileUrl());
        profile.setAvatarPublicId(upload.getPublicId());

        profileRepository.save(profile);

        return userMapper.toUserProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public void updatePassword(ChangePasswordRequest request) {

        User user = getCurrentUser();

        if(request.getNewPassword().equals(request.getOldPassword())){
            throw new BadCredentialsException(
                    "New password must be different with Old password"
            );
        }
        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Invalid password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public PublicUserProfileResponse getUserById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found "
                        )
                );

        Profile profile = getCurrentProfile(userId);

        return buildPublicProfileResponse(
                user,
                profile
        );
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserSearchResponse> findUsersByName(String username, Pageable pageable) {
        Page<UserSearchResponse> userPage = userRepository.searchUsers(
                username,
                Status.ACTIVE,
                pageable
        );

        return userPage;
    }
}
