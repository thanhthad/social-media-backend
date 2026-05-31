CREATE TABLE post_media (
                            media_id BIGINT IDENTITY PRIMARY KEY,
                            post_id BIGINT NOT NULL,

                            url NVARCHAR(255) NOT NULL,
                            public_id NVARCHAR(255),

                            media_type NVARCHAR(20) NOT NULL, -- IMAGE / VIDEO

                            created_at DATETIME2 DEFAULT GETDATE(),

                            CONSTRAINT fk_post_media_post
                                FOREIGN KEY (post_id)
                                    REFERENCES posts(post_id)
                                    ON DELETE CASCADE
);