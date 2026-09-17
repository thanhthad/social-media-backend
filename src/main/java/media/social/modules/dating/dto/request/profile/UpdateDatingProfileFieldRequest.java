package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.dating.enums.DatingProfileFieldName;

/**
 * Request body cho endpoint PATCH /api/dating/me/profile/field.
 * Cho phép cập nhật từng field riêng lẻ trên hồ sơ dating (Facebook-style inline edit).
 *
 * <p>Ví dụ:</p>
 * <pre>{@code
 * { "fieldName": "BIO", "value": "Tôi thích đi phượt và chụp ảnh" }
 * { "fieldName": "HEIGHT", "value": "172" }
 * { "fieldName": "GENDER", "value": "FEMALE" }
 * { "fieldName": "ACTIVE", "value": "true" }
 * }</pre>
 */
@Getter
@Setter
public class UpdateDatingProfileFieldRequest {

    /**
     * Tên field cần cập nhật — xem {@link DatingProfileFieldName} để biết các giá trị hợp lệ.
     */
    @NotNull(message = "fieldName must not be null")
    private DatingProfileFieldName fieldName;

    /**
     * Giá trị mới cho field, luôn truyền dưới dạng String.
     * Service sẽ tự parse/validate theo từng fieldName.
     * Truyền null hoặc chuỗi rỗng để xóa giá trị của field.
     */
    private String value;
}
