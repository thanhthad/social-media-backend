CREATE TABLE users (
                       user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       last_login_at TIMESTAMP,
                       last_active_at TIMESTAMP
);