package media.social.modules.user.service.domain.impl;

import lombok.AllArgsConstructor;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.repository.FriendshipRepository;
import media.social.modules.user.service.domain.FriendShipDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class FriendShipDomainImpl implements FriendShipDomain {

    private final FriendshipRepository friendshipRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean areFriends(Long userId, Long targetUserId) {
        return friendshipRepository.areFriends(
                userId,
                targetUserId,
                FriendshipStatus.ACCEPTED
        );
    }
}
