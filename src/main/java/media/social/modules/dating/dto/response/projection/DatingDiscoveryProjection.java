package media.social.modules.dating.dto.response.projection;

import java.time.LocalDateTime;

public interface DatingDiscoveryProjection {

    Long getUserId();

    String getDisplayName();

    String getAvatarUrl();

    Integer getAge();

    String getCity();

    Double getDistanceKm();

    Long getCommonInterestCount();

    /**
     * Thời điểm người dùng hoạt động lần cuối (từ bảng users.last_active_at).
     * Dùng để tính Recency Score trong thuật toán Discovery.
     */
    LocalDateTime getLastActiveAt();

    /**
     * Số lượng ảnh trong dating profile (COUNT từ dating_profile_photos).
     * Dùng để tính Profile Quality Score.
     */
    Long getPhotoCount();

}