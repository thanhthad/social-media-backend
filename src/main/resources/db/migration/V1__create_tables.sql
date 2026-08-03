-- Thiết lập các bảng
CREATE TABLE roles (
                       role_id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL,
                       description VARCHAR(255)
);

CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE ,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255),
                       provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       status VARCHAR(50),
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP WITH TIME ZONE,
                       last_active_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE email_verification_tokens (
                                           id BIGSERIAL PRIMARY KEY,
                                           user_id BIGINT NOT NULL,
                                           token VARCHAR(255) NOT NULL UNIQUE,
                                           expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                           used BOOLEAN NOT NULL DEFAULT FALSE,
                                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                           CONSTRAINT fk_email_verification_user
                                               FOREIGN KEY (user_id)
                                                   REFERENCES users(user_id)
                                                   ON DELETE CASCADE
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,

                            assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            assigned_by BIGINT,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(user_id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(role_id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_assigned_by
                                FOREIGN KEY (assigned_by)
                                    REFERENCES users(user_id)
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

                       user_id BIGINT NOT NULL
                           REFERENCES users(user_id)
                               ON DELETE CASCADE,

                       content TEXT,

                       visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',

                       comment_count BIGINT NOT NULL DEFAULT 0,

                       reaction_count BIGINT NOT NULL DEFAULT 0,

                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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

                           user_id BIGINT NOT NULL
                               REFERENCES users(user_id)
                                   ON DELETE CASCADE,

                           post_id BIGINT NOT NULL
                               REFERENCES posts(post_id)
                                   ON DELETE CASCADE,

                           type VARCHAR(50) NOT NULL,

                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT unique_user_post_reaction
                               UNIQUE(user_id, post_id)
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

CREATE TABLE hashtags (
                          hashtag_id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE post_hashtags (
                               id BIGSERIAL PRIMARY KEY,

                               post_id BIGINT NOT NULL,
                               hashtag_id BIGINT NOT NULL,

                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_post_hashtag_post
                                   FOREIGN KEY (post_id)
                                       REFERENCES posts(post_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_post_hashtag_hashtag
                                   FOREIGN KEY (hashtag_id)
                                       REFERENCES hashtags(hashtag_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT uk_post_hashtag
                                   UNIQUE(post_id, hashtag_id)
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
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversations (
                               conversation_id BIGSERIAL PRIMARY KEY,

                               type VARCHAR(50) NOT NULL,

                               owner_id BIGINT
                                                REFERENCES users(user_id)
                                                    ON DELETE SET NULL,

                               name VARCHAR(255),

                               avatar_url VARCHAR(255),

                               avatar_public_id VARCHAR(255),

                               last_message_at TIMESTAMP WITH TIME ZONE,

                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
                          message_id BIGSERIAL PRIMARY KEY,

                          conversation_id BIGINT
                              REFERENCES conversations(conversation_id)
                                  ON DELETE CASCADE,

                          sender_id BIGINT
                              REFERENCES users(user_id)
                                  ON DELETE CASCADE,

                          reply_to_message_id BIGINT
                              REFERENCES messages(message_id),

                          content TEXT,

                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                          is_deleted BOOLEAN DEFAULT FALSE
);



CREATE TABLE message_reactions (

                                   reaction_id BIGSERIAL PRIMARY KEY,

                                   user_id BIGINT NOT NULL
                                       REFERENCES users(user_id)
                                           ON DELETE CASCADE,

                                   message_id BIGINT NOT NULL
                                       REFERENCES messages(message_id)
                                           ON DELETE CASCADE,

                                   type VARCHAR(50) NOT NULL,

                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,


                                   CONSTRAINT uk_user_message_reaction
                                       UNIQUE(user_id, message_id)
);

CREATE TABLE conversation_members (
                                      conversation_id BIGINT REFERENCES conversations(conversation_id) ON DELETE CASCADE,
                                      user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,

                                      joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                      last_read_message_id BIGINT
                                          REFERENCES messages(message_id),

                                      PRIMARY KEY (conversation_id, user_id)
);


CREATE TABLE reports (
                         report_id BIGSERIAL PRIMARY KEY,

                         reporter_id BIGINT NOT NULL
                             REFERENCES users(user_id)
                                 ON DELETE CASCADE,

                         post_id BIGINT NOT NULL
                             REFERENCES posts(post_id)
                                 ON DELETE CASCADE,

                         reason TEXT NOT NULL,

                         status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                         reviewed_by BIGINT
                             REFERENCES users(user_id),

                         reviewed_at TIMESTAMP WITH TIME ZONE,

                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uk_report_user_post
                             UNIQUE(reporter_id, post_id)
);

CREATE TABLE blocks (
                        blocker_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                        blocked_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (blocker_id, blocked_id)
);

CREATE TABLE comment_reactions (
                                   reaction_id BIGSERIAL PRIMARY KEY,

                                   user_id BIGINT NOT NULL
                                       REFERENCES users(user_id)
                                           ON DELETE CASCADE,

                                   comment_id BIGINT NOT NULL
                                       REFERENCES comments(comment_id)
                                           ON DELETE CASCADE,

                                   type VARCHAR(50) NOT NULL,

                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uk_user_comment_reaction
                                       UNIQUE(user_id, comment_id)
);

CREATE TABLE message_media (
                               media_id BIGSERIAL PRIMARY KEY,

                               message_id BIGINT NOT NULL
                                   REFERENCES messages(message_id)
                                       ON DELETE CASCADE,

                               url VARCHAR(255),

                               public_id VARCHAR(255),

                               media_type VARCHAR(50),

                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
