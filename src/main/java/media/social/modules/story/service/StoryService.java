package media.social.modules.story.service;

import media.social.modules.story.dto.request.CreateStoryRequest;
import media.social.modules.story.dto.request.UpdateStoryVisibilityRequest;
import media.social.modules.story.dto.response.MyStoryResponse;
import media.social.modules.story.dto.response.StoryFeedResponse;
import media.social.modules.story.dto.response.UserStoryResponse;

import java.util.List;

public interface StoryService {

    void create(
            CreateStoryRequest request
    );

    /**
     * Lấy feed story của người dùng hiện tại.
     *
     * - Chỉ trả về một story mới nhất của mỗi user.
     * - Dùng để hiển thị danh sách story trên feed.
     * - Khi người dùng chọn một user, gọi getUserStoriesBeforeExpire()
     *   để lấy toàn bộ story còn hạn của user đó.
     */
    List<StoryFeedResponse> getFeed();

    /**
     * Lấy toàn bộ story còn hạn của một user mà viewer có quyền xem.
     *
     * - Chỉ lấy các story chưa hết hạn.
     * - Kiểm tra quyền xem dựa trên visibility và quan hệ bạn bè.
     * - Được gọi khi viewer bấm vào một user từ story feed.
     *
     */
    List<UserStoryResponse> getUserStoriesBeforeExpire(
            Long targetUserId
    );

    /**
     * Lấy danh sách story của một user để người khác xem.
     *
     * - Lấy story từ cache.
     * - Kiểm tra quan hệ giữa viewer và target user.
     * - Viewer là chính chủ: xem tất cả story.
     * - Là bạn bè: xem PUBLIC và FRIEND story.
     * - Không phải bạn bè: chỉ xem PUBLIC story.
     */
    List<UserStoryResponse> getUserStories(
            Long targetUserId
    );

    /**
     * Lấy danh sách story của chính user hiện tại
     * kèm thông tin người đã xem và tương tác với từng story.
     *
     * - Lấy story từ cache.
     * - Lấy danh sách view/reaction của các story từ database.
     * - Ghép interactions vào từng story.
     *
     * @return danh sách story của chính user kèm view/reaction
     */
    List<MyStoryResponse> getMyStories();

    void updateVisibility(
            Long storyId,
            UpdateStoryVisibilityRequest request
    );

    void delete(
            Long storyId
    );
}