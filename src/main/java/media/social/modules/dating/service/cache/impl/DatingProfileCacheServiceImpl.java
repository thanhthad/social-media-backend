package media.social.modules.dating.service.cache.impl;

import lombok.RequiredArgsConstructor;
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
            value = "datingProfile",
            key = "#userId"
    )
    @Transactional(readOnly = true)
    public DatingProfileCacheResponse getDatingProfile(Long userId) {

        return datingProfileRepository
                .findDatingProfileCache(userId)
                .orElseThrow(() ->
                        new DatingProfileNotFoundException("Dating profile not found"));
    }

    @Override
    @CacheEvict(
            value = "datingProfile",
            key = "#userId"
    )
    public void evictProfile(Long userId) {
    }
}