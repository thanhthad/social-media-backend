CREATE TABLE users (
                       user_id BIGINT IDENTITY PRIMARY KEY,
                       username NVARCHAR(255) NOT NULL,
                       email NVARCHAR(255) NOT NULL UNIQUE,
                       password_hash NVARCHAR(255) NOT NULL,
                       role NVARCHAR(50) NOT NULL,
                       status NVARCHAR(50) NOT NULL,
                       created_at DATETIME2,
                       updated_at DATETIME2,
                       last_login_at DATETIME2,
                       last_active_at DATETIME2
);