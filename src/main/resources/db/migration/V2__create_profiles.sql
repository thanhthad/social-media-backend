CREATE TABLE profiles (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          user_id BIGINT NOT NULL UNIQUE,
                          full_name VARCHAR(100),
                          avatar_url VARCHAR(255),
                          avatar_public_id VARCHAR(255),
                          bio VARCHAR(255),
                          phone VARCHAR(20),
                          date_of_birth DATE,
                          gender VARCHAR(20),
                          location VARCHAR(255),
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,

                          CONSTRAINT fk_profiles_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(user_id)
                                  ON DELETE CASCADE
);