-- =====================================
-- Dating Profile
-- =====================================
CREATE TABLE dating_profiles (
                                 dating_profile_id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL UNIQUE,
                                 display_name VARCHAR(100),
                                 bio TEXT,
                                 gender VARCHAR(20),
                                 birthday DATE,
                                 height INTEGER,
                                 occupation VARCHAR(100),
                                 education VARCHAR(150),
                                 country VARCHAR(100),
                                 city VARCHAR(100),
                                 district VARCHAR(100),
                                 is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                 visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_dating_profile_user FOREIGN KEY(user_id)
                                     REFERENCES users(user_id)
                                     ON DELETE CASCADE
);

-- =====================================
-- Dating Preference
-- User muốn tìm ai
-- =====================================
CREATE TABLE dating_preferences (
                                    preference_id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL UNIQUE,
                                    min_age INTEGER,
                                    max_age INTEGER,
                                    gender_preference VARCHAR(20),
                                    max_distance INTEGER,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_dating_preference_user FOREIGN KEY(user_id)
                                        REFERENCES users(user_id)
                                        ON DELETE CASCADE
);

-- =====================================
-- Interest
-- Gaming
-- Travel
-- Music
-- Coding
-- =====================================
CREATE TABLE dating_interests (
                                  interest_id BIGSERIAL PRIMARY KEY,
                                  name VARCHAR(100) UNIQUE NOT NULL
);

-- =====================================
-- Mapping profile - interest
-- Many to many
-- =====================================
CREATE TABLE dating_profile_interests (
                                          dating_profile_id BIGINT NOT NULL,
                                          interest_id BIGINT NOT NULL,
                                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY(dating_profile_id, interest_id),
                                          CONSTRAINT fk_profile_interest_profile FOREIGN KEY(dating_profile_id)
                                              REFERENCES dating_profiles(dating_profile_id)
                                              ON DELETE CASCADE,
                                          CONSTRAINT fk_profile_interest_interest FOREIGN KEY(interest_id)
                                              REFERENCES dating_interests(interest_id)
                                              ON DELETE CASCADE
);

-- =====================================
-- Swipe
-- Like / Dislike / Super Like
-- =====================================
CREATE TABLE dating_swipes (
                               swipe_id BIGSERIAL PRIMARY KEY,
                               swiper_id BIGINT NOT NULL,
                               target_id BIGINT NOT NULL,
                               action VARCHAR(30) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_swipe_user FOREIGN KEY(swiper_id)
                                   REFERENCES users(user_id)
                                   ON DELETE CASCADE,
                               CONSTRAINT fk_swipe_target FOREIGN KEY(target_id)
                                   REFERENCES users(user_id)
                                   ON DELETE CASCADE,
                               CONSTRAINT uk_user_target_swipe UNIQUE(swiper_id, target_id)
);

-- =====================================
-- Match
-- Khi 2 user like nhau
-- =====================================
CREATE TABLE dating_matches (
                                match_id BIGSERIAL PRIMARY KEY,
                                user_one_id BIGINT NOT NULL,
                                user_two_id BIGINT NOT NULL,
                                status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                matched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                last_message_at TIMESTAMP WITH TIME ZONE,
                                CONSTRAINT fk_match_user_one FOREIGN KEY(user_one_id)
                                    REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                                CONSTRAINT fk_match_user_two FOREIGN KEY(user_two_id)
                                    REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                                CONSTRAINT uk_match_pair UNIQUE(user_one_id, user_two_id)
);

-- =====================================
-- Dating Report
-- Report riêng cho dating
-- =====================================
CREATE TABLE dating_reports (
                                report_id BIGSERIAL PRIMARY KEY,
                                reporter_id BIGINT NOT NULL,
                                reported_user_id BIGINT NOT NULL,
                                reason TEXT NOT NULL,
                                status VARCHAR(30) DEFAULT 'PENDING',
                                reviewed_by BIGINT,
                                reviewed_at TIMESTAMP WITH TIME ZONE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_dating_report_reporter FOREIGN KEY(reporter_id)
                                    REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                                CONSTRAINT fk_dating_report_user FOREIGN KEY(reported_user_id)
                                    REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                                CONSTRAINT fk_dating_report_admin FOREIGN KEY(reviewed_by)
                                    REFERENCES users(user_id)
);