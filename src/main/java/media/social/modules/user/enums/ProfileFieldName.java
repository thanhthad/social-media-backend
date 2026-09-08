package media.social.modules.user.enums;

/**
 * Danh sách các field được phép cập nhật riêng lẻ qua endpoint
 * PATCH /api/users/me/profile/field
 * Mỗi value tương ứng với 1 cột trong bảng profiles hoặc users.
 */
public enum ProfileFieldName {

    // ── Basic Info ──────────────────────────────────────────────
    /** Họ và tên đầy đủ — max 100 ký tự */
    FULL_NAME,

    /** Bio / giới thiệu bản thân — max 500 ký tự */
    BIO,

    /** Ngày sinh — định dạng ISO 8601 (yyyy-MM-dd), phải là ngày trong quá khứ */
    DATE_OF_BIRTH,

    /** Giới tính — giá trị: MALE | FEMALE | OTHER */
    GENDER,

    // ── Contact ─────────────────────────────────────────────────
    /** Số điện thoại — regex Việt Nam: ^(03|05|07|08|09)[0-9]{8}$ */
    PHONE,

    /** Website cá nhân — max 255 ký tự */
    WEBSITE,

    /** Quốc gia — max 100 ký tự */
    COUNTRY,

    /** Tỉnh / Thành phố — max 100 ký tự */
    CITY,

    /** Quận / Huyện — max 100 ký tự */
    DISTRICT,

    // ── Career ──────────────────────────────────────────────────
    /** Nghề nghiệp — max 100 ký tự */
    OCCUPATION,

    /** Công ty / Nơi làm việc — max 100 ký tự */
    COMPANY,

    /** Học vấn — max 150 ký tự */
    EDUCATION,

    // ── Social Links ────────────────────────────────────────────
    /** Mạng xã hội — JSON object dạng {"facebook":"url","twitter":"url"}, tối đa 5 entries */
    SOCIAL_LINKS,

    // ── Visibility ──────────────────────────────────────────────
    /** Quyền riêng tư trang cá nhân — giá trị: PUBLIC | FRIEND | PRIVATE */
    VISIBILITY
}
