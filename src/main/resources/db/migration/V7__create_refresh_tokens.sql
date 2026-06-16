CREATE TABLE refresh_tokens (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                token VARCHAR(500) NOT NULL UNIQUE,
                                user_id BIGINT NOT NULL,
                                expired_at TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(user_id)
                                        ON DELETE CASCADE
);