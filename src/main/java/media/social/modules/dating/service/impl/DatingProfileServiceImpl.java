package media.social.modules.dating.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.profile.*;
import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.dto.response.profile.DatingProfileResponse;
import media.social.modules.dating.dto.response.profile.MyDatingProfileResponse;
import media.social.modules.dating.dto.response.profile.PublicDatingProfileResponse;
import media.social.modules.dating.dto.response.projection.DatingDistanceProjection;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.service.DatingProfileService;
import media.social.modules.dating.service.cache.DatingProfileCacheService;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import media.social.modules.user.enums.Gender;
import media.social.modules.dating.enums.DatingProfileFieldName;
import java.time.LocalDate;


@Service
@AllArgsConstructor
public class DatingProfileServiceImpl implements DatingProfileService {

    private final DatingProfileRepository datingProfileRepository;
    private final UserRepository userRepository;
    private final DatingProfileCacheService datingProfileCacheService;

    private DatingProfile getDatingProfile(Long userId) {
        return datingProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DatingProfileNotFoundException("Dating profile not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public MyDatingProfileResponse getMe() {

        Long userId = UserContextHolder.getUserId();

        DatingProfileCacheResponse profile =
                datingProfileCacheService.getDatingProfile(userId);

        return MyDatingProfileResponse.builder()
                .username(profile.getUsername())
                .avatarUrl(profile.getAvatarUrl())
                .coverUrl(profile.getCoverUrl())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .gender(profile.getGender())
                .birthday(profile.getBirthday())
                .height(profile.getHeight())
                .occupation(profile.getOccupation())
                .education(profile.getEducation())
                .country(profile.getCountry())
                .city(profile.getCity())
                .district(profile.getDistrict())
                .active(profile.getActive())
                .visibility(profile.getVisibility())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateBasicInfo(UpdateDatingBasicInfoRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setDisplayName(request.getDisplayName());
        profile.setGender(request.getGender());
        profile.setBirthday(request.getBirthday());
        profile.setHeight(request.getHeight());

        datingProfileRepository.save(profile);

        datingProfileCacheService.evictProfile(userId);

        return DatingProfileResponse.builder()
                .displayName(profile.getDisplayName())
                .gender(profile.getGender())
                .birthday(profile.getBirthday())
                .height(profile.getHeight())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateCareer(UpdateDatingCareerRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setOccupation(request.getOccupation());
        profile.setEducation(request.getEducation());

        datingProfileCacheService.evictProfile(userId);
        datingProfileRepository.save(profile);

        return DatingProfileResponse.builder()
                .occupation(profile.getOccupation())
                .education(profile.getEducation())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateLocation(UpdateDatingLocationRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setCountry(request.getCountry());
        profile.setCity(request.getCity());
        profile.setDistrict(request.getDistrict());

        datingProfileRepository.save(profile);

        datingProfileCacheService.evictProfile(userId);

        return DatingProfileResponse.builder()
                .country(profile.getCountry())
                .city(profile.getCity())
                .district(profile.getDistrict())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateCoordinates(
            UpdateDatingCoordinatesRequest request
    ) {
        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());

        datingProfileRepository.save(profile);

        return DatingProfileResponse.builder()
                .country(profile.getCountry())
                .city(profile.getCity())
                .district(profile.getDistrict())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateBio(UpdateDatingBioRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setBio(request.getBio());

        datingProfileCacheService.evictProfile(userId);
        datingProfileRepository.save(profile);

        return DatingProfileResponse.builder()
                .bio(profile.getBio())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateVisibility(UpdateDatingVisibilityRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setVisibility(request.getVisibility());

        datingProfileCacheService.evictProfile(userId);
        datingProfileRepository.save(profile);

        return DatingProfileResponse.builder()
                .visibility(profile.getVisibility())
                .build();
    }

    @Override
    @Transactional
    public DatingProfileResponse updateStatus(UpdateDatingStatusRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        profile.setActive(request.getActive());

        datingProfileCacheService.evictProfile(userId);
        datingProfileRepository.save(profile);

        return DatingProfileResponse.builder()
                .active(profile.getActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicDatingProfileResponse getPublicProfile(Long userId) {

        Long currentUserId = UserContextHolder.getUserId();

        DatingProfileCacheResponse profile =
                datingProfileCacheService.getDatingProfile(userId);

        if (!currentUserId.equals(userId)
                && Visibility.PRIVATE.equals(profile.getVisibility())) {

            return PublicDatingProfileResponse.builder()
                    .avatarUrl(profile.getAvatarUrl())
                    .coverUrl(profile.getCoverUrl())
                    .displayName(profile.getDisplayName())
                    .birthday(profile.getBirthday())
                    .bio(profile.getBio())
                    .build();
        }

        DatingDistanceProjection distance =
                datingProfileRepository.findDistanceBetweenUsers(
                        currentUserId,
                        userId
                );

        return PublicDatingProfileResponse.builder()
                .username(profile.getUsername())
                .avatarUrl(profile.getAvatarUrl())
                .coverUrl(profile.getCoverUrl())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .gender(profile.getGender())
                .birthday(profile.getBirthday())
                .height(profile.getHeight())
                .occupation(profile.getOccupation())
                .education(profile.getEducation())
                .country(profile.getCountry())
                .city(profile.getCity())
                .district(profile.getDistrict())
                .distanceKm(distance.getDistanceKm())
                .build();
    }

    @Override
    @Transactional
    public void deleteProfile() {
        Long userId = UserContextHolder.getUserId();

        DatingProfile datingProfile = getDatingProfile(userId);

        datingProfile.setActive(false);
    }

    /**
     * Cập nhật từng field riêng lẻ trên hồ sơ dating (Facebook-style inline edit).
     * Endpoint: PATCH /api/dating/me/profile/field
     * Value luôn nhận dưới dạng String rồi parse theo từng fieldName.
     */
    @Override
    @Transactional
    public DatingProfileResponse updateDatingProfileField(UpdateDatingProfileFieldRequest request) {

        Long userId = UserContextHolder.getUserId();
        DatingProfile profile = getDatingProfile(userId);

        String raw = request.getValue() != null ? request.getValue().trim() : null;
        DatingProfileResponse.DatingProfileResponseBuilder builder = DatingProfileResponse.builder();

        switch (request.getFieldName()) {

            // ── Basic Info ─────────────────────────────────────────
            case DISPLAY_NAME -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("Display name cannot exceed 100 characters");
                profile.setDisplayName(raw == null || raw.isEmpty() ? null : raw);
                builder.displayName(profile.getDisplayName());
            }

            case BIO -> {
                if (raw != null && raw.length() > 500)
                    throw new IllegalArgumentException("Bio cannot exceed 500 characters");
                profile.setBio(raw == null || raw.isEmpty() ? null : raw);
                builder.bio(profile.getBio());
            }

            case GENDER -> {
                profile.setGender(raw == null || raw.isEmpty() ? null : Gender.valueOf(raw.toUpperCase()));
                builder.gender(profile.getGender());
            }

            case BIRTHDAY -> {
                // Nhận ISO yyyy-MM-dd; null/rỗng → xóa
                LocalDate bday = (raw == null || raw.isEmpty()) ? null : LocalDate.parse(raw);
                if (bday != null && !bday.isBefore(LocalDate.now()))
                    throw new IllegalArgumentException("Birthday must be in the past");
                profile.setBirthday(bday);
                builder.birthday(profile.getBirthday());
            }

            case HEIGHT -> {
                if (raw == null || raw.isEmpty()) {
                    profile.setHeight(null);
                } else {
                    int h = Integer.parseInt(raw);
                    if (h < 100 || h > 250)
                        throw new IllegalArgumentException("Height must be between 100 and 250 cm");
                    profile.setHeight(h);
                }
                builder.height(profile.getHeight());
            }

            // ── Career ──────────────────────────────────────────
            case OCCUPATION -> {
                if (raw != null && raw.length() > 100)
                    throw new IllegalArgumentException("Occupation cannot exceed 100 characters");
                profile.setOccupation(raw == null || raw.isEmpty() ? null : raw);
                builder.occupation(profile.getOccupation());
            }

            case EDUCATION -> {
                if (raw != null && raw.length() > 150)
                    throw new IllegalArgumentException("Education cannot exceed 150 characters");
                profile.setEducation(raw == null || raw.isEmpty() ? null : raw);
                builder.education(profile.getEducation());
            }

            // ── Location ────────────────────────────────────────
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

            // ── Status & Visibility ──────────────────────────────
            case VISIBILITY -> {
                Visibility vis = (raw == null || raw.isEmpty())
                        ? Visibility.PUBLIC
                        : Visibility.valueOf(raw.toUpperCase());
                profile.setVisibility(vis);
                builder.visibility(profile.getVisibility());
            }

            case ACTIVE -> {
                // "true" hoặc "false"
                boolean active = Boolean.parseBoolean(raw);
                profile.setActive(active);
                builder.active(profile.getActive());
            }
        }

        datingProfileRepository.save(profile);
        datingProfileCacheService.evictProfile(userId);

        return builder.build();
    }
}

