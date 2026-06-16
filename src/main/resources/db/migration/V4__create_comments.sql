CREATE TABLE comments (
                          comment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          post_id BIGINT NOT NULL,
                          user_id BIGINT NOT NULL,
                          parent_id BIGINT NULL,
                          content TEXT NOT NULL,
                          created_at TIMESTAMP,

                          CONSTRAINT fk_comments_post
                              FOREIGN KEY (post_id)
                                  REFERENCES posts(post_id)
                                  ON DELETE CASCADE,

                          CONSTRAINT fk_comments_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(user_id),

                          CONSTRAINT fk_comments_parent
                              FOREIGN KEY (parent_id)
                                  REFERENCES comments(comment_id)
                                  ON DELETE NO ACTION
);