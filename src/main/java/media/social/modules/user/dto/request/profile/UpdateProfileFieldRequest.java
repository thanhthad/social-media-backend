package media.social.modules.user.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.user.enums.ProfileFieldName;

/**
 * Request body cho endpoint PATCH /api/users/me/profile/field.
 * Cho phép cập nhật từng field riêng lẻ trên hồ sơ (kiểu Facebook-style).
 *
 * <p>Ví dụ request body:</p>
 * <pre>{@code
 * {
 *   "fieldName": "BIO",
 *   "value": "Tôi là lập trình viên thích cà phê"
 * }
 * }</pre>
 *
 * <p>Với field SOCIAL_LINKS, value là JSON object dạng string:</p>
 * <pre>{@code
 * {
 *   "fieldName": "SOCIAL_LINKS",
 *   "value": "{\"facebook\":\"https://fb.com/...\",\"github\":\"https://github.com/...\"}"
 * }
 * }</pre>
 */
@Getter
@Setter
public class UpdateProfileFieldRequest {

    /**
     * Tên field cần cập nhật — xem {@link ProfileFieldName} để biết các giá trị hợp lệ.
     */
    @NotNull(message = "fieldName must not be null")
    private ProfileFieldName fieldName;

    /**
     * Giá trị mới cho field.
     * Luôn truyền dưới dạng String (hoặc null để xóa).
     * Service sẽ tự parse/validate theo từng fieldName.
     */
    private String value;
}
