package media.social.modules.user.service;

import media.social.modules.user.dto.request.block.BlockRequest;
import media.social.modules.user.dto.response.block.ListUserBlockedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlockService {

    void blockUser(BlockRequest request);

    void unblockUser(Long blockedId);

    boolean checkBlocked( Long userId);

    Page<ListUserBlockedResponse> getBlockedUsers(Pageable pageable);
}