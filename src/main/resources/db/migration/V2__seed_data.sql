-- =====================================================================
-- V2: Seed data
--   - One admin user (email: admin@tempoup.bg)
--   - A starter catalog of sports + skills so the discovery feed and
--     selection screens have content from day one.
--
-- The admin password hash below is BCrypt for the password: "admin123"
-- CHANGE THIS in any real deployment.
-- =====================================================================

INSERT INTO users (id, email, password_hash, role, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@tempoup.bg',
    -- bcrypt hash of 'admin123'
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    TRUE
);

INSERT INTO profiles (user_id, display_name, bio, city)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'TempoUp Admin',
    'Platform administrator account.',
    'Sofia'
);

-- ---- Sports ---------------------------------------------------------
-- Use deterministic UUIDs so we can attach skills below without lookups.
INSERT INTO sports (id, name, description) VALUES
 ('10000000-0000-0000-0000-000000000001', 'Gym',          'General strength training and fitness at the gym.'),
 ('10000000-0000-0000-0000-000000000002', 'Weightlifting','Olympic weightlifting.'),
 ('10000000-0000-0000-0000-000000000003', 'Running',      'Road, trail and track running.'),
 ('10000000-0000-0000-0000-000000000004', 'Tennis',       'Singles and doubles tennis.'),
 ('10000000-0000-0000-0000-000000000005', 'Climbing',     'Indoor and outdoor climbing.'),
 ('10000000-0000-0000-0000-000000000006', 'Skiing',       'Alpine and cross-country skiing.'),
 ('10000000-0000-0000-0000-000000000007', 'Football',     'Association football (soccer).'),
 ('10000000-0000-0000-0000-000000000008', 'Basketball',   'Indoor and outdoor basketball.'),
 ('10000000-0000-0000-0000-000000000009', 'Cycling',      'Road and mountain biking.'),
 ('10000000-0000-0000-0000-00000000000a', 'Yoga',         'All yoga styles.'),
 ('10000000-0000-0000-0000-00000000000b', 'Swimming',     'Pool and open-water swimming.'),
 ('10000000-0000-0000-0000-00000000000c', 'Boxing',       'Boxing and sparring.');

-- ---- Skills ---------------------------------------------------------
-- Gym (many options)
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000001', 'Bench Press'),
 ('10000000-0000-0000-0000-000000000001', 'Squat'),
 ('10000000-0000-0000-0000-000000000001', 'Deadlift'),
 ('10000000-0000-0000-0000-000000000001', 'Overhead Press'),
 ('10000000-0000-0000-0000-000000000001', 'Pull-ups'),
 ('10000000-0000-0000-0000-000000000001', 'Leg Day'),
 ('10000000-0000-0000-0000-000000000001', 'Cardio'),
 ('10000000-0000-0000-0000-000000000001', 'Functional Training'),
 ('10000000-0000-0000-0000-000000000001', 'CrossFit'),
 ('10000000-0000-0000-0000-000000000001', 'Calisthenics');

-- Weightlifting (few options)
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000002', 'Snatch'),
 ('10000000-0000-0000-0000-000000000002', 'Clean and Jerk');

-- Running
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000003', '5K'),
 ('10000000-0000-0000-0000-000000000003', '10K'),
 ('10000000-0000-0000-0000-000000000003', 'Half Marathon'),
 ('10000000-0000-0000-0000-000000000003', 'Marathon'),
 ('10000000-0000-0000-0000-000000000003', 'Trail Running'),
 ('10000000-0000-0000-0000-000000000003', 'Sprinting');

-- Tennis
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000004', 'Singles'),
 ('10000000-0000-0000-0000-000000000004', 'Doubles'),
 ('10000000-0000-0000-0000-000000000004', 'Hitting Practice');

-- Climbing
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000005', 'Bouldering'),
 ('10000000-0000-0000-0000-000000000005', 'Top Rope'),
 ('10000000-0000-0000-0000-000000000005', 'Lead Climbing');

-- Skiing
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000006', 'Alpine'),
 ('10000000-0000-0000-0000-000000000006', 'Cross-Country'),
 ('10000000-0000-0000-0000-000000000006', 'Ski Touring');

-- Cycling
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-000000000009', 'Road'),
 ('10000000-0000-0000-0000-000000000009', 'Mountain Biking'),
 ('10000000-0000-0000-0000-000000000009', 'Gravel');

-- Yoga
INSERT INTO skills (sport_id, name) VALUES
 ('10000000-0000-0000-0000-00000000000a', 'Vinyasa'),
 ('10000000-0000-0000-0000-00000000000a', 'Hatha'),
 ('10000000-0000-0000-0000-00000000000a', 'Power Yoga');
