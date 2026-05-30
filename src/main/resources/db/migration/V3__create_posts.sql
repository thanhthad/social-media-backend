CREATE TABLE posts (
                       post_id BIGINT IDENTITY PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       content NVARCHAR(MAX),
                       image_url NVARCHAR(MAX),
                       image_public_id NVARCHAR(255),
                       created_at DATETIME2,

                       CONSTRAINT fk_posts_user
                           FOREIGN KEY (user_id) REFERENCES users(user_id)
);