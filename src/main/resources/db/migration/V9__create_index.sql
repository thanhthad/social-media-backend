-- USERS
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);

-- POSTS
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);

-- COMMENTS
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);

-- LIKES
CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);

-- FOLLOWS
CREATE INDEX idx_follows_following_id ON follows(following_id);
CREATE INDEX idx_follows_follower_id ON follows(follower_id);

-- REFRESH TOKENS
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- POST MEDIA
CREATE INDEX idx_post_media_post_id ON post_media(post_id);