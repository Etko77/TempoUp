-- =====================================================================
-- V1: Initial schema for TempoUp
-- Requires the PostGIS extension for geography-based proximity matching.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- Users & authentication
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER, ADMIN
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ---------------------------------------------------------------------
-- Profiles  (1:1 with users)
-- ---------------------------------------------------------------------
CREATE TABLE profiles (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name  VARCHAR(100) NOT NULL,
    bio           TEXT,
    date_of_birth DATE,
    gender        VARCHAR(20),                       -- MALE, FEMALE, OTHER, UNSPECIFIED
    photo_url     VARCHAR(512),
    city          VARCHAR(120),
    -- PostGIS point, WGS84 (SRID 4326). NULL until the user shares location.
    location      GEOGRAPHY(Point, 4326),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Spatial index makes "within radius" queries fast.
CREATE INDEX idx_profiles_location ON profiles USING GIST (location);

-- ---------------------------------------------------------------------
-- Sport / Skill hierarchy
-- ---------------------------------------------------------------------
CREATE TABLE sports (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    icon_url    VARCHAR(512),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE skills (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sport_id    UUID NOT NULL REFERENCES sports(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_skill_per_sport UNIQUE (sport_id, name)
);
CREATE INDEX idx_skills_sport ON skills(sport_id);

-- User-selected sports (with proficiency + priority flag)
CREATE TABLE user_sports (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sport_id          UUID NOT NULL REFERENCES sports(id) ON DELETE CASCADE,
    proficiency_level VARCHAR(20) NOT NULL DEFAULT 'BEGINNER', -- BEGINNER, INTERMEDIATE, ADVANCED
    is_priority       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_sport UNIQUE (user_id, sport_id)
);
CREATE INDEX idx_user_sports_user  ON user_sports(user_id);
CREATE INDEX idx_user_sports_sport ON user_sports(sport_id);

-- User-selected skills, scoped to one of their user_sports rows
CREATE TABLE user_skills (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_sport_id UUID NOT NULL REFERENCES user_sports(id) ON DELETE CASCADE,
    skill_id      UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_skill UNIQUE (user_sport_id, skill_id)
);
CREATE INDEX idx_user_skills_user_sport ON user_skills(user_sport_id);
CREATE INDEX idx_user_skills_skill      ON user_skills(skill_id);

-- ---------------------------------------------------------------------
-- Community suggestions (admin moderation queue)
-- ---------------------------------------------------------------------
CREATE TABLE sport_suggestions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type            VARCHAR(10)  NOT NULL,             -- SPORT or SKILL
    parent_sport_id UUID         REFERENCES sports(id) ON DELETE CASCADE, -- NULL for a new sport
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    status          VARCHAR(10)  NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    suggested_by    UUID         REFERENCES users(id) ON DELETE SET NULL,
    reviewed_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
    review_note     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at     TIMESTAMPTZ
);
CREATE INDEX idx_suggestions_status ON sport_suggestions(status);

-- ---------------------------------------------------------------------
-- Swipes & matches
-- ---------------------------------------------------------------------
CREATE TABLE swipes (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    swiper_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    swiped_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    direction  VARCHAR(10) NOT NULL,                  -- LIKE, PASS
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_swipe UNIQUE (swiper_id, swiped_id),
    CONSTRAINT chk_no_self_swipe CHECK (swiper_id <> swiped_id)
);
CREATE INDEX idx_swipes_swiper ON swipes(swiper_id);
CREATE INDEX idx_swipes_swiped ON swipes(swiped_id);

CREATE TABLE matches (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_a_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    matched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Canonical ordering (a < b) guarantees one row per pair.
    CONSTRAINT uq_match UNIQUE (user_a_id, user_b_id),
    CONSTRAINT chk_match_order CHECK (user_a_id < user_b_id)
);
CREATE INDEX idx_matches_user_a ON matches(user_a_id);
CREATE INDEX idx_matches_user_b ON matches(user_b_id);

-- ---------------------------------------------------------------------
-- Chat
-- ---------------------------------------------------------------------
CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id        UUID NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_message_at TIMESTAMPTZ
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
