package media.social.modules.user.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.user.dto.request.block.BlockRequest;
import media.social.modules.user.dto.response.block.ListUserBlockedResponse;
import media.social.modules.user.entity.Block;
import media.social.modules.user.entity.BlockId;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.block.BlockAlreadyExistsException;
import media.social.modules.user.exception.block.BlockNotFoundException;
import media.social.modules.user.repository.BlockRepository;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.BlockService;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockServiceImpl implements BlockService {

    private final BlockRepository blockRepository;
    private final UserServiceDomain userServiceDomain;

    @Override
    public void blockUser(BlockRequest request) {

        Long currentUserId = UserContextHolder.getUserId();

        if (currentUserId.equals(request.getBlockedId())) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        boolean exists = blockRepository
                .findBlock(currentUserId, request.getBlockedId())
                .isPresent();

        if (exists) {
            throw new BlockAlreadyExistsException("Bạn đã block user này rồi mà ?");
        }
        User blocker = userServiceDomain.getByUserId(currentUserId);

        User blocked = userServiceDomain.getByUserId(request.getBlockedId());

        Block block = Block.builder()
                .id(new BlockId(currentUserId, request.getBlockedId()))
                .blocker(blocker)
                .blocked(blocked)
                .build();

        blockRepository.save(block);
    }

    @Override
    public void unblockUser(Long blockedId) {

        Long currentUserId = UserContextHolder.getUserId();

        BlockId id = new BlockId(currentUserId, blockedId);

        if (!blockRepository.existsById(id)){
            throw new BlockNotFoundException("User is not blocked");
        }

        blockRepository.deleteById(id);
    }

    @Override
    public boolean checkBlocked(Long targetUserId) {

        Long currentUserId = UserContextHolder.getUserId();

        boolean blocked = blockRepository
                .findBlock(currentUserId, targetUserId)
                .isPresent();

        return blocked;
    }

    @Override
    public Page<ListUserBlockedResponse> getBlockedUsers(Pageable pageable) {

        Long currentUserId = UserContextHolder.getUserId();

        return blockRepository.findBlockedUsers(currentUserId,pageable);
    }
}