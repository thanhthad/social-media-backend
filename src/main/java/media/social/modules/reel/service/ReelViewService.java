package media.social.modules.reel.service;

import media.social.modules.reel.dto.request.UpdateReelViewRequest;
import media.social.modules.reel.dto.response.ReelViewResponse;

public interface ReelViewService {

    /**
     * Ghi nhận user bắt đầu xem Reel (Start View).
     * Nếu record chưa tồn tại → tạo mới + tăng viewCount trên reel_details.
     * Nếu đã tồn tại → bỏ qua, không tạo thêm.
     *
     * @return record hiện tại của user cho reel này
     */
    ReelViewResponse startView(Long reelId);

    /**
     * Cập nhật tiến trình xem (Update Progress).
     * watchDurationMs chỉ được tăng, không giảm.
     * completed = true thì không reset về false.
     *
     * @return record đã cập nhật
     */
    ReelViewResponse updateProgress(Long reelId, UpdateReelViewRequest request);

    /**
     * Ghi nhận user replay Reel.
     * Tăng replayCount += 1, không tạo record mới.
     *
     * @return record đã cập nhật
     */
    ReelViewResponse replay(Long reelId);
}
