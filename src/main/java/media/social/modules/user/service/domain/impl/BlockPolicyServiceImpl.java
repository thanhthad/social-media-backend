package media.social.modules.user.service.domain.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.user.repository.BlockRepository;
import media.social.modules.user.service.domain.BlockPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockPolicyServiceImpl implements BlockPolicyService {

    private final BlockRepository blockRepository;

    @Override
    public boolean isBlocked(Long viewerId, Long targetId) {

        if (viewerId == null || targetId == null) {
            return false;
        }

        return blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId)
                || blockRepository.existsByBlockerIdAndBlockedId(targetId, viewerId);
    }
}