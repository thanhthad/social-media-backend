CREATE TABLE refresh_tokens (
                                id BIGINT IDENTITY PRIMARY KEY,
                                token NVARCHAR(500) NOT NULL UNIQUE,
                                user_id BIGINT NOT NULL,
                                expired_at DATETIME2 NOT NULL,
                                revoked BIT NOT NULL,
                                created_at DATETIME2,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users(user_id)
);