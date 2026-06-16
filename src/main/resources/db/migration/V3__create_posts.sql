CREATE TABLE posts (
                       post_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       content TEXT,
                       created_at TIMESTAMP,

                       CONSTRAINT fk_posts_user
                           FOREIGN KEY (user_id)
                               REFERENCES users(user_id)
);