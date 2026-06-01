CREATE TABLE likes (
                       id BIGINT IDENTITY PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       post_id BIGINT NOT NULL,
                       created_at DATETIME2,

                       CONSTRAINT fk_likes_user
                           FOREIGN KEY (user_id) REFERENCES users(user_id),

                       CONSTRAINT fk_likes_post
                           FOREIGN KEY (post_id) REFERENCES posts(post_id)
                               ON DELETE CASCADE,

                       CONSTRAINT uq_user_post UNIQUE (user_id, post_id)
);