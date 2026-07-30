-- Add last message reference for conversations
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS last_message_id BIGINT;


ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_last_message
        FOREIGN KEY (last_message_id)
            REFERENCES messages(message_id)
            ON DELETE SET NULL;


-- posts
CREATE INDEX idx_posts_user_created
    ON posts(user_id, created_at DESC);


CREATE INDEX idx_posts_created
    ON posts(created_at DESC);


-- reports
CREATE INDEX idx_reports_post_status
    ON reports(post_id,status);


-- blocks
CREATE INDEX idx_blocks_blocked
    ON blocks(blocked_id);


-- post_hashtags
CREATE INDEX idx_post_hashtags_hashtag
    ON post_hashtags(hashtag_id);


-- saved_posts
CREATE INDEX idx_saved_posts_user_created
    ON saved_posts(user_id, created_at DESC);