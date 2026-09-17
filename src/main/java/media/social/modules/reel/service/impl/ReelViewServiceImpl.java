package media.social.modules.reel.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.entity.Post;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.reel.dto.request.UpdateReelViewRequest;
import media.social.modules.reel.dto.response.ReelViewResponse;
import media.social.modules.reel.entity.ReelView;
import media.social.modules.reel.exception.ReelNotFoundException;
import media.social.modules.reel.exception.ReelViewNotFoundException;
import media.social.modules.reel.repository.ReelDetailRepository;
import media.social.modules.reel.repository.ReelViewRepository;
import media.social.modules.reel.service.ReelViewService;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReelViewServiceImpl implements ReelViewService {

    private final ReelViewRepository reelViewRepository;
    private final ReelDetailRepository reelDetailRepository;
    private final PostRepository postRepository;
    private final UserServiceDomain userServiceDomain;

    // ─── START VIEW ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReelViewResponse startView(Long reelId) {

        Long userId = UserContextHolder.getUserId();

        // 1. Kiểm tra reel tồn tại
        Post reel = postRepository.findByIdWithUser(reelId, PostType.REEL)
                .orElseThrow(() -> new ReelNotFoundException("Reel not found: " + reelId));

        // 2. Kiểm tra đã có record chưa (mỗi user + reel chỉ có 1 record)
        if (reelViewRepository.existsByReelIdAndUserId(reelId, userId)) {
            // Đã từng xem → trả về record hiện tại, không tạo mới
            ReelView existing = reelViewRepository.findByReelIdAndUserId(reelId, userId)
                    .orElseThrow(() -> new ReelViewNotFoundException("View record not found"));
            return toResponse(existing);
        }

        // 3. Tạo record mới
        User user = userServiceDomain.getByUserId(userId);

        ReelView view = ReelView.builder()
                .reel(reel)
                .user(user)
                .build();

        reelViewRepository.save(view);

        // 4. Tăng viewCount trên reel_details
        reelDetailRepository.incrementViewCount(reelId);

        return toResponse(view);
    }

    // ─── UPDATE PROGRESS ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReelViewResponse updateProgress(Long reelId, UpdateReelViewRequest request) {

        Long userId = UserContextHolder.getUserId();

        // Bắt buộc phải có record (phải gọi startView trước)
        ReelView view = reelViewRepository.findByReelIdAndUserId(reelId, userId)
                .orElseThrow(() -> new ReelViewNotFoundException(
                        "View record not found. Please call start view first."
                ));

        /*
         * Cập nhật trực tiếp qua native query để đảm bảo:
         * - watchDurationMs chỉ tăng  → GREATEST(watch_duration_ms, :durationMs)
         * - completed không reset      → completed OR :completed
         */
        reelViewRepository.updateProgress(
                reelId,
                userId,
                request.getWatchDurationMs(),
                request.getCompleted()
        );

        // Reload entity để trả về trạng thái mới nhất
        view = reelViewRepository.findByReelIdAndUserId(reelId, userId)
                .orElseThrow(() -> new ReelViewNotFoundException("View record not found after update"));

        return toResponse(view);
    }

    // ─── REPLAY ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReelViewResponse replay(Long reelId) {

        Long userId = UserContextHolder.getUserId();

        // Bắt buộc phải có record (phải gọi startView trước)
        int updated = reelViewRepository.incrementReplayCount(reelId, userId);

        if (updated == 0) {
            throw new ReelViewNotFoundException(
                    "View record not found. Please call start view first."
            );
        }

        ReelView view = reelViewRepository.findByReelIdAndUserId(reelId, userId)
                .orElseThrow(() -> new ReelViewNotFoundException("View record not found"));

        return toResponse(view);
    }

    // ─── PRIVATE HELPER ────────────────────────────────────────────────────────

    private ReelViewResponse toResponse(ReelView view) {
        return ReelViewResponse.builder()
                .viewId(view.getViewId())
                .reelId(view.getReel().getId())
                .userId(view.getUser() != null ? view.getUser().getId() : null)
                .watchDurationMs(view.getWatchDurationMs())
                .completed(view.getCompleted())
                .replayCount(view.getReplayCount())
                .createdAt(view.getCreatedAt())
                .build();
    }
}
