package media.social.modules.dating.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.infrastructure.redis.RedisCacheNames;
import media.social.modules.dating.dto.projection.DatingProfileCacheProjection;
import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.service.cache.DatingProfileCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingProfileCacheServiceImpl
        implements DatingProfileCacheService {

    private final DatingProfileRepository datingProfileRepository;

    @Override
    @Cacheable(
            value = RedisCacheNames.DATING_PROFILE,
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public DatingProfileCacheResponse getDatingProfile(Long userId) {

        DatingProfileCacheProjection projection = datingProfileRepository
                .findDatingProfileCache(userId)
                .orElseThrow(() ->
                        new DatingProfileNotFoundException("Dating profile not found"));

        return new DatingProfileCacheResponse(
                projection.getUsername(),
                projection.getAvatarUrl(),
                projection.getCoverUrl(),
                projection.getDisplayName(),
                projection.getBio(),
                projection.getGender(),
                projection.getBirthday(),
                projection.getHeight(),
                projection.getOccupation(),
                projection.getEducation(),
                projection.getCountry(),
                projection.getCity(),
                projection.getDistrict(),
                projection.getActive(),
                projection.getVisibility(),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }

    @Override
    @CacheEvict(
            value = RedisCacheNames.DATING_PROFILE,
            key = "#userId"
    )
    public void evictProfile(Long userId) {
    }
}