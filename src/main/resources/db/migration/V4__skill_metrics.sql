-- =====================================================================
-- V4: Skill metrics
--   - Each skill gains a metric_type describing the numbers users record.
--   - user_skills gains the recorded values + a starred flag.
--   - sport_suggestions can carry a proposed metric_type for skill ideas.
-- =====================================================================

ALTER TABLE skills
    ADD COLUMN metric_type VARCHAR(24) NOT NULL DEFAULT 'NONE';

ALTER TABLE user_skills
    ADD COLUMN weight_kg        DOUBLE PRECISION,
    ADD COLUMN reps             INTEGER,
    ADD COLUMN distance_km      DOUBLE PRECISION,
    ADD COLUMN duration_seconds INTEGER,
    ADD COLUMN speed_kmh        DOUBLE PRECISION,
    ADD COLUMN is_starred       BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_user_skills_starred ON user_skills(is_starred) WHERE is_starred;

ALTER TABLE sport_suggestions
    ADD COLUMN metric_type VARCHAR(24);

-- ---- Categorize the seeded catalog ----------------------------------
-- Strength: barbell lifts measured by weight x reps.
UPDATE skills SET metric_type = 'STRENGTH'
WHERE name IN ('Bench Press', 'Squat', 'Deadlift', 'Overhead Press', 'Snatch', 'Clean and Jerk');

-- Endurance (reps): bodyweight volume.
UPDATE skills SET metric_type = 'ENDURANCE_REPS'
WHERE name IN ('Pull-ups', 'Calisthenics');

-- Endurance (distance + optional time): runs and rides.
UPDATE skills SET metric_type = 'ENDURANCE_DISTANCE'
WHERE name IN ('Cardio', '5K', '10K', 'Half Marathon', 'Marathon', 'Trail Running',
               'Road', 'Mountain Biking', 'Gravel');

-- Speed: top speed in km/h.
UPDATE skills SET metric_type = 'SPEED'
WHERE name IN ('Sprinting');

-- Everything else keeps the default 'NONE'.
