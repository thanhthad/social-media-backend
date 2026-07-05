package media.social.modults.user.service;

import media.social.modults.user.dto.request.block.BlockRequest;
import media.social.modults.user.dto.response.block.BlockCheckResponse;

import java.util.List;

public interface BlockService {

    void blockUser(Long currentUserId, BlockRequest request);

    void unblockUser(Long currentUserId, Long blockedId);

    BlockCheckResponse checkBlocked(Long currentUserId, Long userId);

    List<UserSimpleResponse> getBlockedUsers(Long currentUserId);
}