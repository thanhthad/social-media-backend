
-- =========================
-- User search
-- =========================

CREATE INDEX idx_users_username_trgm
    ON users
    USING gin(username gin_trgm_ops);



-- =========================
-- Post content search
-- =========================

CREATE INDEX idx_posts_content_trgm
    ON posts
    USING gin(content gin_trgm_ops);