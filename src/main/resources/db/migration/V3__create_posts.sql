CREATE TABLE posts (
                       post_id BIGINT IDENTITY PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       content NVARCHAR(MAX),
                       created_at DATETIME2,

                       CONSTRAINT fk_posts_user
                           FOREIGN KEY (user_id) REFERENCES users(user_id)
);