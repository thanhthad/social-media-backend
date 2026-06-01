CREATE TABLE profiles (
                          id BIGINT IDENTITY PRIMARY KEY,
                          user_id BIGINT NOT NULL UNIQUE,
                          full_name NVARCHAR(100),
                          avatar_url NVARCHAR(255),
                          avatar_public_id NVARCHAR(255),
                          bio NVARCHAR(255),
                          phone NVARCHAR(20),
                          date_of_birth DATE,
                          gender NVARCHAR(20),
                          location NVARCHAR(255),
                          created_at DATETIME2,
                          updated_at DATETIME2,

                          CONSTRAINT fk_profiles_user
                              FOREIGN KEY (user_id) REFERENCES users(user_id)
                                  ON DELETE CASCADE
);