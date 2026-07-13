package media.social.modults.user.service;

import media.social.modults.user.dto.request.block.BlockRequest;
import media.social.modults.user.dto.response.block.BlockCheckResponse;
import media.social.modults.user.dto.response.block.ListUserBlockedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BlockService {

    void blockUser(BlockRequest request);

    void unblockUser(Long blockedId);

    boolean checkBlocked( Long userId);

    Page<ListUserBlockedResponse> getBlockedUsers(Pageable pageable);
}