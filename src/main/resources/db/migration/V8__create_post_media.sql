CREATE TABLE post_media (
                            media_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            post_id BIGINT NOT NULL,

                            url VARCHAR(255) NOT NULL UNIQUE,
                            public_id VARCHAR(255) NOT NULL,

                            media_type VARCHAR(20) NOT NULL,

                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_post_media_post
                                FOREIGN KEY (post_id)
                                    REFERENCES posts(post_id)
                                    ON DELETE CASCADE
);