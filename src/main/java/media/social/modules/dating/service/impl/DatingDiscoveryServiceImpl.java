package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.response.discovery.DatingDiscoveryResponse;
import media.social.modules.dating.dto.response.projection.DatingDiscoveryProjection;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.exception.profile.CoordinatesNotFoundException;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.service.DatingDiscoveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DatingDiscoveryServiceImpl implements DatingDiscoveryService {

    private final DatingProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DatingDiscoveryResponse> findDiscovery(Pageable pageable) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile datingProfile =
                profileRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new DatingProfileNotFoundException(
                                        "You must have dating profile"
                                ));

        if (datingProfile.getLatitude() == null
                || datingProfile.getLongitude() == null) {

            throw new CoordinatesNotFoundException(
                    "You must set your coordinate"
            );
        }

        if (datingProfile.getBirthday() == null) {

            throw new BadRequestException(
                    "You must set your birthday"
            );
        }

        Page<DatingDiscoveryProjection> discoveryProjections =
                profileRepository.findDiscoveryCandidates(
                        userId,
                        pageable
                );

        return discoveryProjections.map(candidate -> {

            int compatibilityScore =
                    calculateCompatibilityScore(
                            candidate,
                            datingProfile
                    );

            return DatingDiscoveryResponse.builder()
                    .userId(candidate.getUserId())
                    .displayName(candidate.getDisplayName())
                    .avatarUrl(candidate.getAvatarUrl())
                    .age(candidate.getAge())
                    .city(candidate.getCity())
                    .distanceKm(candidate.getDistanceKm())
                    .compatibilityScore(compatibilityScore)
                    .build();
        });
    }

    /**
     * Công thức Compatibility Score tổng hợp:
     *
     *   Score = Interest×15% + Age×20% + Distance×40% + Recency×15% + Quality×10%
     *
     * - Interest  : số sở thích chung (0→100)
     * - Age       : độ gần độ tuổi (0→100, trừ 10 mỗi năm chênh lệch)
     * - Distance  : khoảng cách km (0→100, trừ 2 mỗi km)
     * - Recency   : mức độ hoạt động gần đây (0→100, dựa trên last_active_at)
     * - Quality   : chất lượng hồ sơ (0→100, dựa trên số ảnh)
     */
    private int calculateCompatibilityScore(
            DatingDiscoveryProjection candidate,
            DatingProfile currentProfile
    ) {
        double interestScore  = calculateInterestScore(candidate.getCommonInterestCount());
        double ageScore       = calculateAgeScore(candidate.getAge(), currentProfile.getBirthday());
        double distanceScore  = calculateDistanceScore(candidate.getDistanceKm());
        double recencyScore   = calculateRecencyScore(candidate.getLastActiveAt());
        double qualityScore   = calculateProfileQualityScore(candidate.getPhotoCount());

        double score =
                interestScore  * 0.15
                + ageScore     * 0.20
                + distanceScore* 0.40
                + recencyScore * 0.15
                + qualityScore * 0.10;

        return (int) Math.round(score);
    }

    // ─── Sub-score calculators ──────────────────────────────────────────────────

    private double calculateInterestScore(Long commonInterestCount) {
        if (commonInterestCount == null || commonInterestCount <= 0) {
            return 0.0;
        }
        return switch (commonInterestCount.intValue()) {
            case 1  -> 50.0;
            case 2  -> 75.0;
            default -> 100.0;
        };
    }

    private double calculateAgeScore(Integer targetAge, LocalDate currentBirthday) {
        if (targetAge == null || currentBirthday == null) {
            return 0.0;
        }
        int currentAge = Period.between(currentBirthday, LocalDate.now()).getYears();
        int ageDifference = Math.abs(currentAge - targetAge);
        return Math.max(0.0, 100.0 - (ageDifference * 10.0));
    }

    private double calculateDistanceScore(Double distanceKm) {
        if (distanceKm == null) {
            return 0.0;
        }
        return Math.max(0.0, 100.0 - (distanceKm * 2.0));
    }

    /**
     * Recency Score — Ưu tiên người dùng đang tích cực online.
     *
     * Thang điểm:
     *   - Online trong vòng 1 giờ  : 100
     *   - Online trong vòng 24 giờ : 80
     *   - Online trong vòng 7 ngày  : 50
     *   - Online trong vòng 30 ngày : 20
     *   - Không có thông tin / > 30 ngày : 0
     */
    private double calculateRecencyScore(LocalDateTime lastActiveAt) {
        if (lastActiveAt == null) {
            return 0.0;
        }
        long hoursAgo = ChronoUnit.HOURS.between(lastActiveAt, LocalDateTime.now());
        if (hoursAgo <= 1)   return 100.0;
        if (hoursAgo <= 24)  return 80.0;
        if (hoursAgo <= 168) return 50.0;  // 7 ngày
        if (hoursAgo <= 720) return 20.0;  // 30 ngày
        return 0.0;
    }

    /**
     * Profile Quality Score — Ưu tiên hồ sơ có nhiều ảnh (dấu hiệu chân thực, đầu tư).
     *
     * Thang điểm:
     *   - 0 ảnh   : 0   (hồ sơ ảo / chưa hoàn thiện)
     *   - 1 ảnh   : 30
     *   - 2 ảnh   : 60
     *   - 3+ ảnh  : 100 (tiêu chuẩn lý tưởng)
     */
    private double calculateProfileQualityScore(Long photoCount) {
        if (photoCount == null || photoCount <= 0) {
            return 0.0;
        }
        return switch (photoCount.intValue()) {
            case 1  -> 30.0;
            case 2  -> 60.0;
            default -> 100.0;  // 3 ảnh trở lên
        };
    }
}

