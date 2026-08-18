package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.preference.UpdateDatingPreferenceRequest;
import media.social.modules.dating.dto.response.preference.DatingPreferenceResponse;
import media.social.modules.dating.entity.DatingPreference;
import media.social.modules.dating.exception.preference.PreferenceNotFoundException;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.repository.DatingPreferenceRepository;
import media.social.modules.dating.service.DatingPreferenceService;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingPreferenceServiceImpl implements DatingPreferenceService {

    private final DatingPreferenceRepository datingPreferenceRepository;
    private final UserRepository userRepository;
    private final UserServiceDomain userServiceDomain;

    @Override
    @Transactional
    public DatingPreferenceResponse createPreference(UpdateDatingPreferenceRequest request) {

        Long userId = UserContextHolder.getUserId();

        if (datingPreferenceRepository.existsByUserId(userId)) {
            throw new BadRequestException("Dating preference already exists");
        }

        validateAge(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        DatingPreference preference = DatingPreference.builder()
                .user(user)
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .genderPreference(request.getGenderPreference())
                .maxDistance(request.getMaxDistance())
                .build();

        datingPreferenceRepository.save(preference);

        return DatingPreferenceResponse.builder()
                .minAge(preference.getMinAge())
                .maxAge(preference.getMaxAge())
                .genderPreference(preference.getGenderPreference())
                .maxDistance(preference.getMaxDistance())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DatingPreferenceResponse getMyPreference() {

        Long userId = UserContextHolder.getUserId();

        DatingPreference preference = getDatingPreference(userId);

        return DatingPreferenceResponse.builder()
                .minAge(preference.getMinAge())
                .maxAge(preference.getMaxAge())
                .genderPreference(preference.getGenderPreference())
                .maxDistance(preference.getMaxDistance())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public DatingPreferenceResponse updatePreference(UpdateDatingPreferenceRequest request) {

        Long userId = UserContextHolder.getUserId();

        validateAge(request);

        DatingPreference preference = getDatingPreference(userId);

        preference.setMinAge(request.getMinAge());
        preference.setMaxAge(request.getMaxAge());
        preference.setGenderPreference(request.getGenderPreference());
        preference.setMaxDistance(request.getMaxDistance());

        datingPreferenceRepository.save(preference);

        return DatingPreferenceResponse.builder()
                .minAge(preference.getMinAge())
                .maxAge(preference.getMaxAge())
                .genderPreference(preference.getGenderPreference())
                .maxDistance(preference.getMaxDistance())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private DatingPreference getDatingPreference(Long userId) {

        User user = userServiceDomain.getByUserId(userId);

        return datingPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> datingPreferenceRepository.save(
                        DatingPreference.builder()
                                .user(user)
                                .build())
                );
    }

    private void validateAge(UpdateDatingPreferenceRequest request) {

        if (request.getMinAge() > request.getMaxAge()) {
            throw new BadRequestException("Minimum age cannot greater than maximum age");
        }
    }
}