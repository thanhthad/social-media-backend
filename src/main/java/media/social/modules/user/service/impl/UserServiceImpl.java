package media.social.modules.user.service.impl;
import media.social.modules.post.enums.MediaType;
import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.user.dto.request.profile.*;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.cache.UserFollowStatCacheResponse;
import media.social.modules.user.dto.response.user.*;
import media.social.modules.user.exception.user.UserAlreadyExistsException;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.service.FollowService;
import media.social.modules.user.service.cache.UserCacheService;
import media.social.modules.user.service.cache.UserProfileCacheService;
import media.social.modules.user.service.domain.UserRoleServiceDomain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import media.social.modules.file.image.dto.response.UploadFileResponse;
import media.social.modules.file.image.service.CloudinaryService;
import media.social.modules.auth.Enum.Status;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.dto.request.user.UpdateProfileRequest;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.mapper.ProfileMapper;
import media.social.modules.user.mapper.UserMapper;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfileRepository profileRepository;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleServiceDomain userRoleServiceDomain;
    private final FollowService followService;
    private final UserProfileCacheService userProfileCacheService;
    private final UserCacheService userCacheService;

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponse getMe() {

        Long userId = UserContextHolder.getUserId();

        PublicUserProfileCacheResponse cache =
                userProfileCacheService.getUserProfile(userId);

        UserFollowStatCacheResponse followStat =
                userProfileCacheService.getFollowStat(userId);

        return MyProfileResponse.builder()
                .userId(cache.getId())

                .email(cache.getEmail())

                .username(cache.getUsername())

                .avatarUrl(cache.getAvatarUrl())
                .coverUrl(cache.getCoverUrl())

                .bio(cache.getBio())
                .fullName(cache.getFullName())

                .website(cache.getWebsite())

                .phone(cache.getPhone())

                .dateOfBirth(cache.getDateOfBirth())
                .gender(cache.getGender())

                .country(cache.getCountry())
                .city(cache.getCity())
                .district(cache.getDistrict())

                .occupation(cache.getOccupation())
                .company(cache.getCompany())
                .education(cache.getEducation())

                .profileVisibility(cache.getProfileVisibility())

                .socialLinks(cache.getSocialLinks())

                .createdAt(cache.getCreatedAt())
                .updatedAt(cache.getUpdatedAt())

                .totalFollower(
                        followStat.getTotalFollower()
                )

                .totalFollowing(
                        followStat.getTotalFollowing()
                )

                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateBasicProfile(UpdateBasicProfileRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );

        Profile profile = user.getProfile();

        profile.setFullName(request.getFullName());
        profile.setBio(request.getBio());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());

        profileRepository.save(profile);

        userCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .fullName(profile.getFullName())
                .bio(profile.getBio())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateContact(UpdateContactRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );

        Profile profile = user.getProfile();

        profile.setPhone(request.getPhone());
        profile.setWebsite(request.getWebsite());
        profile.setCountry(request.getCountry());
        profile.setCity(request.getCity());
        profile.setDistrict(request.getDistrict());

        profileRepository.save(profile);

        userCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .phone(profile.getPhone())
                .website(profile.getWebsite())
                .country(profile.getCountry())
                .city(profile.getCity())
                .district(profile.getDistrict())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateCareer(UpdateCareerRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );

        Profile profile = user.getProfile();

        profile.setOccupation(request.getOccupation());
        profile.setCompany(request.getCompany());
        profile.setEducation(request.getEducation());

        profileRepository.save(profile);

        userCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .occupation(profile.getOccupation())
                .company(profile.getCompany())
                .education(profile.getEducation())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateSocialLinks(UpdateSocialLinksRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );

        Profile profile = user.getProfile();

        profile.setSocialLinks(request.getSocialLinks());

        profileRepository.save(profile);

        userCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .socialLinks(profile.getSocialLinks())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateProfileVisibility(
            UpdateProfileVisibilityRequest request
    ) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );

        Profile profile = user.getProfile();

        profile.setProfileVisibility(
                request.getVisibility()
        );

        profileRepository.save(profile);

        userCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .profileVisibility(profile.getProfileVisibility())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserName(UpdateUsernameRequest request) {
        boolean checked = userRepository.existsByUsername(request.getUserName());
        if(checked){
            throw new UserAlreadyExistsException("Username already exists");
        }
        Long userId = UserContextHolder.getUserId();
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User Not Found")
        );
        if(user.getEmailVerified() != false){
            throw new AccessDeniedException("Only new user must be use API ");
        }
        user.setUsername(request.getUserName());
        userRepository.save(user);

        return UserProfileResponse.builder()
                .username(request.getUserName())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateCover(UpdateCoverRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );

        Profile profile = user.getProfile();

        cloudinaryService.validateFile(
                request.getFile(),
                MediaType.IMAGE
        );

        UploadFileResponse upload =
                cloudinaryService.uploadFile(
                        request.getFile(),
                        "covers",
                        MediaType.IMAGE
                );

        if(profile.getCoverPublicId() != null){

            cloudinaryService.deleteFile(
                    profile.getCoverPublicId(),
                    MediaType.IMAGE
            );
        }

        profile.setCoverUrl(
                upload.getFileUrl()
        );

        profile.setCoverPublicId(
                upload.getPublicId()
        );

        profileRepository.save(profile);

        userCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .coverUrl(profile.getCoverUrl())
                .build();
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(UpdateAvatarRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        Profile profile = user.getProfile();

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

        userCacheService.evictProfile(user.getId());

        return ProfileResponse.builder()
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }

    @Override
    @Transactional
    public void updatePassword(ChangePasswordRequest request) {

        User user = userRepository.findById(UserContextHolder.getUserId()).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if(!user.getProvider().equals(AuthProvider.LOCAL)){
            throw new BadCredentialsException(
                    "You login by google so you don't need have password"
            );
        }

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
    @Transactional(readOnly = true)
    public PublicProfileResponse getUserById(Long userId) {

        Long currentUserId =
                UserContextHolder.getUserId();

        if (Objects.equals(userId, currentUserId)) {
            throw new UserAlreadyExistsException(
                    "Please use /me endpoint"
            );
        }

        validateCanViewProfile(
                currentUserId,
                userId
        );

        PublicUserProfileCacheResponse cache =
                userProfileCacheService.getUserProfile(userId);

        UserFollowStatCacheResponse followStat =
                userProfileCacheService.getFollowStat(userId);

        Boolean following =
                followService.isFollowing(userId);

        return PublicProfileResponse.builder()
                .userId(cache.getId())
                .username(cache.getUsername())
                .avatarUrl(cache.getAvatarUrl())
                .coverUrl(cache.getCoverUrl())
                .bio(cache.getBio())
                .fullName(cache.getFullName())
                .website(cache.getWebsite())
                .dateOfBirth(cache.getDateOfBirth())
                .gender(cache.getGender())
                .country(cache.getCountry())
                .city(cache.getCity())
                .district(cache.getDistrict())
                .occupation(cache.getOccupation())
                .company(cache.getCompany())
                .education(cache.getEducation())
                .socialLinks(cache.getSocialLinks())
                .createdAt(cache.getCreatedAt())
                .updatedAt(cache.getUpdatedAt())
                .totalFollower(
                        followStat.getTotalFollower()
                )
                .totalFollowing(
                        followStat.getTotalFollowing()
                )
                .isFollowing(following)
                .build();
    }

    private void validateCanViewProfile(
            Long currentUserId,
            Long targetUserId
    ) {

        boolean currentIsAdmin =
                userRoleServiceDomain.hasRole(
                        currentUserId,
                        RoleName.ADMIN
                );

        if (currentIsAdmin) {
            return;
        }

        boolean currentIsModerator =
                userRoleServiceDomain.hasRole(
                        currentUserId,
                        RoleName.MODERATOR
                );

        boolean targetIsAdmin =
                userRoleServiceDomain.hasRole(
                        targetUserId,
                        RoleName.ADMIN
                );

        boolean targetIsModerator =
                userRoleServiceDomain.hasRole(
                        targetUserId,
                        RoleName.MODERATOR
                );

        if (currentIsModerator && targetIsAdmin) {

            throw new AccessDeniedException(
                    "Moderator cannot view admin profile"
            );
        }

        if (!currentIsModerator
                && (targetIsAdmin || targetIsModerator)) {

            throw new AccessDeniedException(
                    "You cannot view this profile"
            );
        }
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
