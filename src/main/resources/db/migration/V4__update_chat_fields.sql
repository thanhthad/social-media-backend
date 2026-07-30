-- Add last message reference for conversations
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS last_message_id BIGINT;


ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_last_message
        FOREIGN KEY (last_message_id)
            REFERENCES messages(message_id)
            ON DELETE SET NULL;

-- POST

CREATE INDEX idx_posts_user_created
    ON posts(user_id, created_at DESC);


CREATE INDEX idx_posts_visibility_created
    ON posts(visibility, created_at DESC);


-- FOLLOW

CREATE INDEX idx_follow_following
    ON follows(following_id);


-- BLOCK

CREATE INDEX idx_blocks_blocked
    ON blocks(blocked_id);


-- REPORT

CREATE INDEX idx_reports_post_status
    ON reports(post_id,status);


-- MEDIA

CREATE INDEX idx_post_media_post
    ON post_media(post_id);


-- COMMENTS

CREATE INDEX idx_comments_post_created
    ON comments(post_id,created_at DESC);


-- HASHTAG

CREATE INDEX idx_hashtags_lower_name
    ON hashtags(LOWER(name));


-- SEARCH CONTENT PostgreSQL

CREATE EXTENSION pg_trgm;

CREATE INDEX idx_posts_content_search
    ON posts
    USING gin(content gin_trgm_ops);