package media.social.modults.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import media.social.modults.file.image.dto.response.UploadImageResponse;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.user.Enum.Status;
import media.social.modults.user.dto.request.self.ChangePasswordRequest;
import media.social.modults.user.dto.request.self.UpdateAvatarRequest;
import media.social.modults.user.dto.request.self.UpdateProfileRequest;
import media.social.modults.user.dto.response.pub.PublicUserProfileResponse;
import media.social.modults.user.dto.response.pub.UserSearchResponse;
import media.social.modults.user.dto.response.self.UserProfileResponse;
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
                                "User not found with id: " + userId
                        )
                );
    }

    private Profile getCurrentProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ProfileNotFoundException(
                                "Profile not found with userId: " + userId
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

        cloudinaryService.validateImage(request.getFile());

        UploadImageResponse upload =
                cloudinaryService.uploadImage(
                        request.getFile(),
                        "avatars"
                );

        if (profile.getAvatarPublicId() != null) {
            cloudinaryService.deleteImage(
                    profile.getAvatarPublicId()
            );
        }

        profile.setAvatarUrl(upload.getImageUrl());
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

    @Override
    public PublicUserProfileResponse getUserById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        Profile profile = getCurrentProfile(userId);

        return buildPublicProfileResponse(
                user,
                profile
        );
    }

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
