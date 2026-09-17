CREATE TABLE reel_details (
                              reel_id BIGINT PRIMARY KEY
                                  REFERENCES posts(post_id)
                                      ON DELETE CASCADE,

                              duration_seconds INTEGER NOT NULL,

                              width INTEGER NOT NULL,

                              height INTEGER NOT NULL,

                              thumbnail_url VARCHAR(500) NOT NULL,

                              thumbnail_public_id VARCHAR(255) NOT NULL,

                              view_count BIGINT NOT NULL DEFAULT 0,

                              share_count BIGINT NOT NULL DEFAULT 0,

                              created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT chk_reel_duration
                                  CHECK (duration_seconds > 0),

                              CONSTRAINT chk_reel_width
                                  CHECK (width > 0),

                              CONSTRAINT chk_reel_height
                                  CHECK (height > 0),

                              CONSTRAINT chk_reel_view_count
                                  CHECK (view_count >= 0),

                              CONSTRAINT chk_reel_share_count
                                  CHECK (share_count >= 0)
);


CREATE TABLE reel_views (
                            view_id BIGSERIAL PRIMARY KEY,

                            reel_id BIGINT NOT NULL
                                REFERENCES posts(post_id)
                                    ON DELETE CASCADE,

                            user_id BIGINT
                                           REFERENCES users(user_id)
                                               ON DELETE SET NULL,

                            watch_duration_ms BIGINT NOT NULL DEFAULT 0,

                            completed BOOLEAN NOT NULL DEFAULT FALSE,

                            replay_count INTEGER NOT NULL DEFAULT 0,

                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT uk_reel_views_reel_user
                                UNIQUE (reel_id, user_id)
);


