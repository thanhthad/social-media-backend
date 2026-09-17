package media.social.modules.dating.enums;

/**
 * Danh sách các field được phép cập nhật riêng lẻ qua endpoint
 * PATCH /api/dating/me/profile/field
 * Mỗi value tương ứng với 1 cột trong bảng dating_profiles.
 */
public enum DatingProfileFieldName {

    // ── Basic Info ──────────────────────────────────────────────
    /** Tên hiển thị trong hồ sơ dating — max 100 ký tự */
    DISPLAY_NAME,

    /** Giới thiệu bản thân trong hồ sơ dating — max 500 ký tự */
    BIO,

    /** Giới tính — giá trị: MALE | FEMALE | OTHER */
    GENDER,

    /** Ngày sinh — định dạng ISO 8601 (yyyy-MM-dd), phải là ngày trong quá khứ */
    BIRTHDAY,

    /** Chiều cao (cm) — từ 100 đến 250 */
    HEIGHT,

    // ── Career ──────────────────────────────────────────────────
    /** Nghề nghiệp — max 100 ký tự */
    OCCUPATION,

    /** Học vấn — max 150 ký tự */
    EDUCATION,

    // ── Location ────────────────────────────────────────────────
    /** Quốc gia — max 100 ký tự */
    COUNTRY,

    /** Tỉnh / Thành phố — max 100 ký tự */
    CITY,

    /** Quận / Huyện — max 100 ký tự */
    DISTRICT,

    // ── Status & Visibility ─────────────────────────────────────
    /** Quyền riêng tư hồ sơ dating — giá trị: PUBLIC | FRIEND | PRIVATE */
    VISIBILITY,

    /** Trạng thái kích hoạt — true/false */
    ACTIVE
}
