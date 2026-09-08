package media.social.modules.user.service.impl;
import media.social.modules.post.enums.MediaType;
import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.user.dto.projection.MutualFriendCountProjection;
import media.social.modules.user.dto.projection.UserSearchProjection;
import media.social.modules.user.dto.request.profile.*;
import media.social.modules.user.dto.request.user.UpdateUsernameRequest;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.friend.FriendshipCountResponse;
import media.social.modules.user.dto.response.friend.MutualFriendCountResponse;
import media.social.modules.user.dto.response.user.*;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.user.exception.user.UserAlreadyExistsException;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.service.cache.UserCacheService;
import media.social.modules.user.service.cache.UserProfileCacheService;
import media.social.modules.user.service.domain.BlockPolicyService;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserRoleServiceDomain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.auth.Enum.Status;
import media.social.modules.user.dto.request.user.ChangePasswordRequest;
import media.social.modules.user.dto.request.user.UpdateAvatarRequest;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import media.social.modules.post.enums.PostType;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.repository.FriendshipRepository;
import java.util.Optional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;
import media.social.modules.user.enums.ProfileFieldName;
import java.time.LocalDate;
import java.util.Map;


@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final MediaUploadService mediaUploadService;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleServiceDomain userRoleServiceDomain;
    private final FriendShipDomain friendShipDomain;
    private final UserProfileCacheService userProfileCacheService;
    private final BlockPolicyService blockPolicyService;
    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponse getMe() {

        Long userId = UserContextHolder.getUserId();

        PublicUserProfileCacheResponse cache =
                userProfileCacheService.getUserProfile(userId);

        FriendshipCountResponse totalFriend =
                userProfileCacheService.getTotalFriend(userId);

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
                .visibility(cache.getVisibility())
                .socialLinks(cache.getSocialLinks())
                .createdAt(cache.getCreatedAt())
                .updatedAt(cache.getUpdatedAt())
                .totalFriend(totalFriend.getTotalFriends())
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

        userProfileCacheService.evictProfile(userId);

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

        userProfileCacheService.evictProfile(userId);

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

        userProfileCacheService.evictProfile(userId);

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

        userProfileCacheService.evictProfile(userId);

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

        profile.setVisibility(
                request.getVisibility()
        );

        profileRepository.save(profile);

        userProfileCacheService.evictProfile(userId);

        return ProfileResponse.builder()
                .profileVisibility(profile.getVisibility())
                .build();
    }

    /**
     * Cập nhật từng field riêng lẻ trên Profile (Facebook-style).
     * Endpoint: PATCH /api/users/me/profile/field
     * Value luôn nhận dưới dạng String rồi parse theo từng fieldName.
     * Trả về ProfileResponse chứa đúng field vừa cập nhật.
     */
    @Override
    @Transactional
    public ProfileResponse updateProfileField(UpdateProfileFieldRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Profile profile = user.getProfile();
        String raw = request.getValue() != null ? request.getValue().trim() : null;

        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder();

        switch (request.getFieldName()) {

            // ── Basic Info ─────────────────────────────────────────────
            case FULL_NAME -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("Full name cannot exceed 100 characters");
                profile.setFullName(raw);
                builder.fullName(profile.getFullName());
            }

            case BIO -> {
                if (raw != null && raw.length() > 500)
                    throw new IllegalArgumentException("Bio cannot exceed 500 characters");
                profile.setBio(raw);
                builder.bio(profile.getBio());
            }

            case DATE_OF_BIRTH -> {
                // Nhận dạng ISO yyyy-MM-dd; null hoặc rỗng → xóa ngày sinh
                LocalDate dob = (raw == null || raw.isEmpty()) ? null : LocalDate.parse(raw);
                if (dob != null && !dob.isBefore(LocalDate.now()))
                    throw new IllegalArgumentException("Date of birth must be in the past");
                profile.setDateOfBirth(dob);
                builder.dateOfBirth(profile.getDateOfBirth());
            }

            case GENDER -> {
                // Null/rỗng → xóa gender
                profile.setGender(raw == null || raw.isEmpty() ? null : Gender.valueOf(raw.toUpperCase()));
                builder.gender(profile.getGender());
            }

            // ── Contact ───────────────────────────────────────────────
            case PHONE -> {
                if (raw != null && !raw.isEmpty() && !raw.matches("^(03|05|07|08|09)[0-9]{8}$"))
                    throw new IllegalArgumentException("Invalid phone number");
                profile.setPhone(raw == null || raw.isEmpty() ? null : raw);
                builder.phone(profile.getPhone());
            }

            case WEBSITE -> {
                if (raw != null && raw.length() > 255)
                    throw new IllegalArgumentException("Website cannot exceed 255 characters");
                profile.setWebsite(raw == null || raw.isEmpty() ? null : raw);
                builder.website(profile.getWebsite());
            }

            case COUNTRY -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("Country cannot exceed 100 characters");
                profile.setCountry(raw == null || raw.isEmpty() ? null : raw);
                builder.country(profile.getCountry());
            }

            case CITY -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("City cannot exceed 100 characters");
                profile.setCity(raw == null || raw.isEmpty() ? null : raw);
                builder.city(profile.getCity());
            }

            case DISTRICT -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("District cannot exceed 100 characters");
                profile.setDistrict(raw == null || raw.isEmpty() ? null : raw);
                builder.district(profile.getDistrict());
            }

            // ── Career ────────────────────────────────────────────────
            case OCCUPATION -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("Occupation cannot exceed 100 characters");
                profile.setOccupation(raw == null || raw.isEmpty() ? null : raw);
                builder.occupation(profile.getOccupation());
            }

            case COMPANY -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("Company cannot exceed 100 characters");
                profile.setCompany(raw == null || raw.isEmpty() ? null : raw);
                builder.company(profile.getCompany());
            }

            case EDUCATION -> {
                if (raw != null && raw.length() > 150)
                    throw new IllegalArgumentException("Education cannot exceed 150 characters");
                profile.setEducation(raw == null || raw.isEmpty() ? null : raw);
                builder.education(profile.getEducation());
            }

            // ── Social Links ──────────────────────────────────────────
            case SOCIAL_LINKS -> {
                // Value là JSON string: {"facebook":"https://...","twitter":"https://..."}
                if (raw == null || raw.isEmpty()) {
                    profile.setSocialLinks(null);
                } else {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, String> links = mapper.readValue(
                                raw, new TypeReference<Map<String, String>>() {}
                        );
                        if (links.size() > 5)
                            throw new IllegalArgumentException("Maximum 5 social links allowed");
                        profile.setSocialLinks(links);
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid social links format. Expected JSON object.");
                    }
                }
                builder.socialLinks(profile.getSocialLinks());
            }

            // ── Visibility ────────────────────────────────────────────
            case VISIBILITY -> {
                Visibility vis = (raw == null || raw.isEmpty())
                        ? Visibility.PUBLIC
                        : Visibility.valueOf(raw.toUpperCase());
                profile.setVisibility(vis);
                builder.profileVisibility(profile.getVisibility());
            }
        }

        profileRepository.save(profile);
        userProfileCacheService.evictProfile(userId);

        return builder.build();
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

        UploadFileResponse upload =
                mediaUploadService.upload(request.getFile(), MediaUploadContext.PROFILE);

        if (profile.getCoverPublicId() != null) {
            mediaUploadService.delete(profile.getCoverPublicId(), MediaType.IMAGE);
        }

        profile.setCoverUrl(
                upload.getFileUrl()
        );

        profile.setCoverPublicId(
                upload.getPublicId()
        );

        profileRepository.save(profile);

        userProfileCacheService.evictProfile(userId);

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

        UploadFileResponse upload =
                mediaUploadService.upload(request.getFile(), MediaUploadContext.PROFILE);

        if (profile.getAvatarPublicId() != null) {
            mediaUploadService.delete(profile.getAvatarPublicId(), MediaType.IMAGE);
        }

        profile.setAvatarUrl(upload.getFileUrl());
        profile.setAvatarPublicId(upload.getPublicId());

        profileRepository.save(profile);

        userProfileCacheService.evictProfile(user.getId());

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

        Long currentUserId = UserContextHolder.getUserId();
        boolean isSelf = Objects.equals(userId, currentUserId);

        if (!isSelf) {
            if (blockPolicyService.isBlocked(currentUserId, userId)) {
                throw new UserBlockedException(
                        "You cannot view this user's profile."
                );
            }

            validateCanViewProfile(
                    currentUserId,
                    userId
            );
        }

        PublicUserProfileCacheResponse cache =
                userProfileCacheService.getUserProfile(userId);

        FriendshipCountResponse totalFriend =
                userProfileCacheService.getTotalFriend(userId);

        MutualFriendCountResponse mutualFriendCount = isSelf
                ? MutualFriendCountResponse.builder().totalMutualCount(0L).build()
                : userProfileCacheService.getTotalMutualFriend(
                        currentUserId,
                        userId
                );

        List<String> listAvatar = isSelf
                ? List.of()
                : userProfileCacheService.getMutualFriendAvatars(
                        currentUserId,
                        userId
                );

        boolean friends = !isSelf && friendShipDomain.areFriends(
                currentUserId,
                userId
        );

        boolean canViewFullProfile = isSelf || switch (cache.getVisibility()) {
            case PUBLIC -> true;
            case FRIEND -> friends;
            case PRIVATE -> false;
        };

        PublicProfileResponse.PublicProfileResponseBuilder builder =
                PublicProfileResponse.builder()
                        .userId(cache.getId())
                        .username(cache.getUsername())
                        .avatarUrl(cache.getAvatarUrl())
                        .coverUrl(cache.getCoverUrl())
                        .totalMutualFriendAvatars(listAvatar)
                        .totalFriend(totalFriend.getTotalFriends())
                        .totalMutualCount(mutualFriendCount.getTotalMutualCount())
                        .isFriend(friends);

        if (canViewFullProfile) {
            builder
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
                    .updatedAt(cache.getUpdatedAt());
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long targetUserId) {
        Long currentUserId = UserContextHolder.getUserId();

        PublicUserProfileCacheResponse cache =
                userProfileCacheService.getUserProfile(targetUserId);

        FriendshipCountResponse totalFriend =
                userProfileCacheService.getTotalFriend(targetUserId);

        long totalPost = postRepository.countByUserIdAndPostType(targetUserId, PostType.POST);
        long totalReel = postRepository.countByUserIdAndPostType(targetUserId, PostType.REEL);
        long totalLikes = postRepository.sumReactionsByUserId(targetUserId);

        String friendshipStatus = "NONE";
        if (Objects.equals(targetUserId, currentUserId)) {
            friendshipStatus = "SELF";
        } else if (blockPolicyService.isBlocked(currentUserId, targetUserId)) {
            friendshipStatus = "BLOCKED";
        } else {
            Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(currentUserId, targetUserId);
            if (friendship.isPresent()) {
                Friendship f = friendship.get();
                if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                    friendshipStatus = "FRIENDS";
                } else if (f.getStatus() == FriendshipStatus.PENDING) {
                    if (Objects.equals(f.getUserOne().getId(), currentUserId)) {
                        friendshipStatus = "PENDING_SENT";
                    } else {
                        friendshipStatus = "PENDING_RECEIVED";
                    }
                }
            }
        }

        return UserStatsResponse.builder()
                .userId(cache.getId())
                .username(cache.getUsername())
                .fullName(cache.getFullName())
                .avatarUrl(cache.getAvatarUrl())
                .totalPost(totalPost)
                .totalReel(totalReel)
                .totalFriend(totalFriend.getTotalFriends())
                .totalLikesReceived(totalLikes)
                .friendshipStatus(friendshipStatus)
                .isOnline(true)
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
    public Page<UserSearchResponse> findUsersByName(
            String username,
            Pageable pageable
    ) {

        Long viewerId = UserContextHolder.getUserId();

        Page<UserSearchProjection> projections =
                userRepository.searchUsers(
                        username,
                        Status.ACTIVE.name(),
                        viewerId,
                        pageable
                );

        return projections.map(user ->
                UserSearchResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .fullName(user.getFullName())
                        .build()
        );
    }
}
