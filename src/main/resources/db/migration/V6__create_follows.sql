CREATE TABLE follows (
                         follower_id BIGINT NOT NULL,
                         following_id BIGINT NOT NULL,
                         created_at DATETIME2,

                         CONSTRAINT pk_follows PRIMARY KEY (follower_id, following_id),

                         CONSTRAINT fk_follows_follower
                             FOREIGN KEY (follower_id)
                                 REFERENCES users(user_id)
                                 ON DELETE CASCADE,

                         CONSTRAINT fk_follows_following
                             FOREIGN KEY (following_id)
                                 REFERENCES users(user_id)
                                 ON DELETE NO ACTION
);