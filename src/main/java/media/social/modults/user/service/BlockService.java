package media.social.modults.user.service;

import media.social.modults.user.dto.request.block.BlockRequest;
import media.social.modults.user.dto.response.block.BlockCheckResponse;
import media.social.modults.user.dto.response.block.ListUserBlockedResponse;

import java.util.List;

public interface BlockService {

    void blockUser(BlockRequest request);

    void unblockUser(Long blockedId);

    BlockCheckResponse checkBlocked( Long userId);

    List<ListUserBlockedResponse> getBlockedUsers();
}