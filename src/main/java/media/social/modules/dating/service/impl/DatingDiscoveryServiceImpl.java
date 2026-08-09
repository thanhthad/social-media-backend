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
import java.time.Period;

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
     * V1 Compatibility Score
     *
     * Interest  = 10%
     * Age       = 30%
     * Distance  = 60%
     */
    private int calculateCompatibilityScore(
            DatingDiscoveryProjection candidate,
            DatingProfile currentProfile
    ) {

        double interestScore =
                calculateInterestScore(
                        candidate.getCommonInterestCount()
                );

        double ageScore =
                calculateAgeScore(
                        candidate.getAge(),
                        currentProfile.getBirthday()
                );

        double distanceScore =
                calculateDistanceScore(
                        candidate.getDistanceKm()
                );

        double score =
                interestScore * 0.10
                        + ageScore * 0.30
                        + distanceScore * 0.60;

        return (int) Math.round(score);
    }

    /**
     * Interest Score
     *
     * 0 interests -> 0
     * 1 interest  -> 50
     * 2 interests -> 75
     * 3+ interests -> 100
     */
    private double calculateInterestScore(
            Long commonInterestCount
    ) {

        if (commonInterestCount == null
                || commonInterestCount <= 0) {

            return 0.0;
        }

        return switch (commonInterestCount.intValue()) {
            case 1 -> 50.0;
            case 2 -> 75.0;
            default -> 100.0;
        };
    }

    /**
     * Age Score
     *
     * 0 tuổi lệch -> 100
     * 1 tuổi     -> 90
     * 2 tuổi     -> 80
     * ...
     * 10+ tuổi   -> 0
     */
    private double calculateAgeScore(
            Integer targetAge,
            LocalDate currentBirthday
    ) {

        if (targetAge == null
                || currentBirthday == null) {

            return 0.0;
        }

        int currentAge =
                Period.between(
                        currentBirthday,
                        LocalDate.now()
                ).getYears();

        int ageDifference =
                Math.abs(currentAge - targetAge);

        return Math.max(
                0.0,
                100.0 - (ageDifference * 10.0)
        );
    }

    /**
     * Distance Score
     *
     * 0 km  -> 100
     * 5 km  -> 90
     * 10 km -> 80
     * ...
     * 50+ km -> 0
     */
    private double calculateDistanceScore(
            Double distanceKm
    ) {

        if (distanceKm == null) {
            return 0.0;
        }

        return Math.max(
                0.0,
                100.0 - (distanceKm * 2.0)
        );
    }
}
