package media.social.modules.reel.service;

import media.social.modules.reel.dto.request.CreateReelRequest;
import media.social.modules.reel.dto.request.UpdateReelContentRequest;
import media.social.modules.reel.dto.response.ReelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReelService {

    /**
     * Tạo reel mới: upload video + thumbnail → lưu Post (type=REEL) + ReelDetail.
     */
    void createReel(CreateReelRequest request);

    /**
     * Lấy chi tiết một reel theo reelId.
     * Kiểm tra visibility + block trước khi trả về.
     */
    ReelResponse getReelById(Long reelId);

    /**
     * Feed reel của người dùng: bài của bản thân + bạn bè.
     */
    Page<ReelResponse> getReelFeed(Pageable pageable);

    /**
     * Explore reel: public reel từ người lạ (không phải bạn bè).
     */
    Page<ReelResponse> getReelExplore(Pageable pageable);

    /**
     * Lấy tất cả reel của bản thân (mọi visibility).
     */
    Page<ReelResponse> getMyReels(Pageable pageable);

    /**
     * Lấy reel của một user khác (lọc theo visibility + block).
     */
    Page<ReelResponse> getUserReels(Long userId, Pageable pageable);

    /**
     * Cập nhật content hoặc visibility của reel.
     * Chỉ chủ sở hữu mới được phép.
     */
    void updateReel(Long reelId, UpdateReelContentRequest request);

    /**
     * Xoá reel: xoá Post + ReelDetail + file trên Cloudinary.
     * Chỉ chủ sở hữu mới được phép.
     */
    void deleteReel(Long reelId);

    /**
     * Tăng view count (gọi khi user xem reel).
     */
    void incrementViewCount(Long reelId);

    /**
     * Tăng share count (gọi khi user chia sẻ reel).
     */
    void incrementShareCount(Long reelId);
}