CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       status VARCHAR(50),
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP WITH TIME ZONE,
                       last_active_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE roles (
                       role_id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL,
                       description VARCHAR(255)
);

CREATE TABLE conversations (
                               conversation_id BIGSERIAL PRIMARY KEY,
                               type VARCHAR(50),
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hashtags (
                          hashtag_id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE profiles (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
                          full_name VARCHAR(100),
                          avatar_url VARCHAR(255),
                          avatar_public_id VARCHAR(255),
                          cover_url VARCHAR(255),
                          bio VARCHAR(255),
                          phone VARCHAR(20),
                          date_of_birth DATE,
                          gender VARCHAR(10),
                          location VARCHAR(255),
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
                       post_id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                       content TEXT,
                       visibility VARCHAR(20),
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                            role_id BIGINT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
                            assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            assigned_by BIGINT REFERENCES users(user_id),
                            PRIMARY KEY (user_id, role_id)
);

CREATE TABLE post_media (
                            media_id BIGSERIAL PRIMARY KEY,
                            post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE,
                            url VARCHAR(255),
                            public_id VARCHAR(255),
                            media_type VARCHAR(50),
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comments (
                          comment_id BIGSERIAL PRIMARY KEY,
                          post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE,
                          user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                          parent_id BIGINT REFERENCES comments(comment_id),
                          content TEXT,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reactions (
                           reaction_id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                           post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE,
                           type VARCHAR(50),
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE follows (
                         follower_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                         following_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (follower_id, following_id)
);

CREATE TABLE saved_posts (
                             user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                             post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (user_id, post_id)
);

CREATE TABLE post_hashtags (
                               post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE,
                               hashtag_id BIGINT REFERENCES hashtags(hashtag_id) ON DELETE CASCADE,
                               PRIMARY KEY (post_id, hashtag_id)
);

CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token VARCHAR(255) NOT NULL,
                                user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                                expired_at TIMESTAMP WITH TIME ZONE,
                                revoked BOOLEAN DEFAULT FALSE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
                               notification_id BIGSERIAL PRIMARY KEY,
                               receiver_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                               sender_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                               entity_type VARCHAR(50),
                               entity_id BIGINT,
                               type VARCHAR(50),
                               message TEXT,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversation_members (
                                      conversation_id BIGINT REFERENCES conversations(conversation_id) ON DELETE CASCADE,
                                      user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                                      joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages (
                          message_id BIGSERIAL PRIMARY KEY,
                          conversation_id BIGINT REFERENCES conversations(conversation_id) ON DELETE CASCADE,
                          sender_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                          content TEXT,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          read_at TIMESTAMP WITH TIME ZONE,
                          is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE reports (
                         report_id BIGSERIAL PRIMARY KEY,
                         reporter_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                         post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE,
                         reason TEXT,
                         status VARCHAR(50),
                         reviewed_by BIGINT REFERENCES users(user_id),
                         reviewed_at TIMESTAMP WITH TIME ZONE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE blocks (
                        blocker_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                        blocked_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (blocker_id, blocked_id)
);