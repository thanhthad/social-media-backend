-- =====================================
-- Story
-- =====================================

CREATE TABLE stories (
                         story_id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         content TEXT,
                         visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
                         expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_story_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(user_id)
                                 ON DELETE CASCADE,

                         CONSTRAINT ck_story_visibility
                             CHECK (visibility IN ('PUBLIC', 'FRIEND', 'PRIVATE')),

                         CONSTRAINT ck_story_expiration
                             CHECK (expires_at > created_at)
);
-- =====================================
-- 2. Story Media
-- =====================================
CREATE TABLE story_media (
                             media_id BIGSERIAL PRIMARY KEY,
                             story_id BIGINT NOT NULL UNIQUE,
                             url VARCHAR(255) NOT NULL,
                             public_id VARCHAR(255) NOT NULL,
                             media_type VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_story_media_story
                                 FOREIGN KEY (story_id)
                                     REFERENCES stories(story_id)
                                     ON DELETE CASCADE,

                             CONSTRAINT ck_story_media_type
                                 CHECK (media_type IN ('IMAGE', 'VIDEO'))
);
-- =====================================
-- 3. Story Views
-- =====================================
CREATE TABLE story_views (
                             story_id BIGINT NOT NULL,
                             viewer_id BIGINT NOT NULL,
                             viewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (story_id, viewer_id),
                             CONSTRAINT fk_story_view_story
                                 FOREIGN KEY (story_id)
                                     REFERENCES stories(story_id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_story_viewer
                                 FOREIGN KEY (viewer_id)
                                     REFERENCES users(user_id)
                                     ON DELETE CASCADE
);
-- =====================================
-- 4. Story Reactions
-- =====================================
CREATE TABLE story_reactions (
                                 reaction_id BIGSERIAL PRIMARY KEY,
                                 story_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,
                                 type VARCHAR(50) NOT NULL,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_story_reaction_story
                                     FOREIGN KEY (story_id)
                                         REFERENCES stories(story_id)
                                         ON DELETE CASCADE,
                                 CONSTRAINT fk_story_reaction_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(user_id)
                                         ON DELETE CASCADE,
                                 CONSTRAINT uk_story_user_reaction
                                     UNIQUE (story_id, user_id)
);
-- =====================================
-- Indexes
-- =====================================
-- Story feed:
-- Find active stories of a user
CREATE INDEX idx_stories_user_expires_at
    ON stories(user_id, expires_at);
-- Find active stories ordered by creation time
CREATE INDEX idx_stories_expires_at_created_at
    ON stories(expires_at, created_at);
-- Story media lookup
CREATE INDEX idx_story_media_story_id
    ON story_media(story_id);
-- Story viewer lookup
CREATE INDEX idx_story_views_story_id
    ON story_views(story_id);
CREATE INDEX idx_story_views_viewer_id
    ON story_views(viewer_id);
-- Story reaction lookup
CREATE INDEX idx_story_reactions_story_id
    ON story_reactions(story_id);
CREATE INDEX idx_story_reactions_user_id
    ON story_reactions(user_id);