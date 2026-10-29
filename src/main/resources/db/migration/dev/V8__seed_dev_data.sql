-- Eema V1 — Development seed data
-- 10 Amsterdam venues for local development and staging.
-- IMPORTANT: Run only in non-production environments.
-- In production: skip this file or use a separate seed process.

-- Seed a test owner user
-- Password: password123
-- BCrypt hash (strength 10)

INSERT INTO users (id,
                   email,
                   password_hash,
                   role,
                   full_name,
                   email_verified)
VALUES ('00000000-0000-0000-0000-000000000001',
        'owner@eema-dev.local',
        '$2a$10$92IXUNpkjO0rQO5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'OWNER',
        'Dev Owner',
        TRUE) ON CONFLICT DO NOTHING;


-- Seed a test scout user

INSERT INTO users (id,
                   email,
                   password_hash,
                   role,
                   full_name,
                   email_verified)
VALUES ('00000000-0000-0000-0000-000000000002',
        'scout@eema-dev.local',
        '$2a$10$92IXUNpkjO0rQO5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'SCOUT',
        'Dev Scout',
        TRUE) ON CONFLICT DO NOTHING;


-- 10 seed venues with varied tags, neighbourhoods, sizes, and rates

INSERT INTO venues (owner_id,
                    name,
                    neighbourhood,
                    size_m2,
                    day_rate_eur,
                    tags,
                    description,
                    status)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Westergasfabriek Studio A',
        'Westerpark',
        280,
        1800.00,
        ARRAY['industrial', 'daylight', 'high-ceiling', 'parking'],
        'Former gas factory converted into a raw industrial studio with north-facing skylights.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Jordaan Loft',
        'Jordaan',
        120,
        950.00,
        ARRAY['loft', 'daylight', 'canal-view', 'quiet'],
        'Top-floor loft with original wooden beams and canal views. Perfect for editorial shoots.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'NDSM Raw Space',
        'NDSM',
        600,
        2400.00,
        ARRAY['industrial', 'raw', 'large', 'high-ceiling', 'outside-area'],
        'Shipyard hall with exposed steel trusses. 9m ceilings. Loading dock access.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'De Pijp Atelier',
        'De Pijp',
        85,
        680.00,
        ARRAY['atelier', 'daylight', 'white-walls', 'quiet'],
        'Classic Amsterdam atelier on the second floor. Clean white walls. South-facing windows.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Oud-West Kitchen Studio',
        'Oud-West',
        95,
        750.00,
        ARRAY['kitchen', 'daylight', 'marble', 'styled'],
        'Purpose-built kitchen studio. Full marble worktops. Professional oven. Great for food and beverage.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Zuidas Glass Tower Floor 22',
        'Zuidas',
        180,
        1400.00,
        ARRAY['corporate', 'panoramic-view', 'modern', 'city-view'],
        'Corner office floor with 270° city views. Ideal for commercial and fashion editorial.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Amsterdam-Noord Warehouse',
        'Noord',
        450,
        2100.00,
        ARRAY['warehouse', 'industrial', 'raw', 'loading-dock', 'parking'],
        'Post-war warehouse. Polished concrete floors. Two loading bays. 7m clearance.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Plantage Garden Studio',
        'Plantage',
        65,
        580.00,
        ARRAY['garden', 'daylight', 'greenery', 'quiet', 'outdoor'],
        'Ground floor studio with private garden. Ideal for lifestyle and beauty shoots.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Harbour View Penthouse',
        'Oostelijk Havengebied',
        200,
        1600.00,
        ARRAY['penthouse', 'harbour-view', 'modern', 'daylight', 'rooftop'],
        'Contemporary penthouse with wraparound rooftop terrace and harbour views.',
        'ACTIVE'),

       ('00000000-0000-0000-0000-000000000001',
        'Vondelpark Townhouse',
        'Oud-Zuid',
        160,
        1200.00,
        ARRAY['residential', 'classic', 'daylight', 'styled', 'quiet'],
        'Beautifully dressed townhouse near Vondelpark. Fully styled. Ideal for luxury brand shoots.',
        'ACTIVE')
    ON CONFLICT DO NOTHING;
