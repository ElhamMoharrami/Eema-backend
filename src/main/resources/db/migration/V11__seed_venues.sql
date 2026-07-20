-- V11__seed_venues.sql
-- Seed the database with 20-30 diverse venues for realistic matching
-- IMPORTANT: Run only in non-production environments.
-- In production: skip this file or use a separate seed process.

-- We'll use the same test owner user id as in V8__seed_dev_data.sql
-- '00000000-0000-0000-0000-000000000001'

-- 1. Warehouse – large, industrial, low cost
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Industrial Loft Warehouse', 'South London', 800, 1200.00,
        ARRAY['warehouse','industrial','high ceilings','raw concrete'],
        ARRAY['/photos/warehouse1.jpg','/photos/warehouse2.jpg'],
        NULL,
        'Spacious raw warehouse with 6m ceilings, loading dock, and natural skylights.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 2. Gallery – mid‑size, white walls, premium
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Contemporary Art Gallery', 'West End', 250, 2500.00,
        ARRAY['gallery','white walls','track lighting','security'],
        ARRAY['/photos/gallery1.jpg'],
        NULL,
        'White‑wall gallery with adjustable track lighting and 24‑hr security.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 3. Rooftop – outdoor, city view
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Rooftop Terrace with Skyline View', 'Shoreditch', 150, 3000.00,
        ARRAY['rooftop','outdoor','city view','bar'],
        ARRAY['/photos/rooftop1.jpg'],
        NULL,
        'Private rooftop with 360° London skyline, built‑in bar, and power outlets.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 4. Townhouse – intimate, period features
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Georgian Townhouse Drawing Room', 'Kensington', 80, 1800.00,
        ARRAY['townhouse','period','chandelier','parquet floor'],
        ARRAY['/photos/townhouse1.jpg'],
        NULL,
        'Elegant drawing room with original chandelier and parquet flooring.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 5. Studio – small, blackout, flexible
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Blackout Photography Studio', 'Camden', 60, 900.00,
        ARRAY['studio','blackout','seamless paper','cyclorama'],
        ARRAY['/photos/studio1.jpg'],
        NULL,
        'Light‑tight studio with seamless paper backdrop and adjustable grid ceiling.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 6. Pop‑up Retail Unit
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Pop‑up Retail Unit', 'Brixton', 40, 500.00,
        ARRAY['pop-up','retail','short-term','basic lighting'],
        ARRAY['/photos/popup1.jpg'],
        NULL,
        'Short‑term retail unit with basic lighting and grid power.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 7. Event Space – large, flexible
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Flexible Event Space', 'Canary Wharf', 1000, 3500.00,
        ARRAY['event space','modular','high ceilings','loading dock'],
        ARRAY['/photos/event1.jpg','/photos/event2.jpg'],
        NULL,
        'Large column-free space with 10m ceilings, flexible partitioning, and professional rigging points.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 8. Industrial Kitchen – large, equipped
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Industrial Kitchen Studio', 'Whitechapel', 120, 2000.00,
        ARRAY['kitchen','industrial','stainless steel','ventilation'],
        ARRAY['/photos/kitchen1.jpg'],
        NULL,
        'Commercial-grade kitchen with professional equipment, ample prep space, and easy cleanup.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 9. Classic Salon – intimate, styled
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Vintage Beauty Salon', 'Covent Garden', 70, 1200.00,
        ARRAY['salon','vintage','styling chairs','natural light'],
        ARRAY['/photos/salon1.jpg'],
        NULL,
        'Beautifully preserved 1920s salon with original fixtures and plenty of natural light.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 10. Artist Studio – creative, inspiring
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'North London Artist Studio', 'Islington', 90, 800.00,
        ARRAY['artist studio','creative','inspiring','exposed brick'],
        ARRAY['/photos/artist1.jpg'],
        NULL,
        'Bright, inspiring studio space with exposed brick walls and high windows perfect for natural light.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 11. Minimalist Gallery – clean, modern
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Minimalist White Cube Gallery', 'Mayfair', 180, 2200.00,
        ARRAY['gallery','minimalist','white walls','concrete floors'],
        ARRAY['/photos/minimal1.jpg'],
        NULL,
        'Clean, modern gallery space with pristine white walls and polished concrete floors.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 12. Industrial Loft – raw, spacious
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Shoreditch Industrial Loft', 'Shoreditch', 300, 1600.00,
        ARRAY['loft','industrial','exposed pipes','concrete'],
        ARRAY['/photos/loft1.jpg'],
        NULL,
        'Raw industrial loft with exposed pipes, concrete floors, and large factory windows.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 13. Garden Studio – outdoor, serene
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Secret Garden Studio', 'Hampstead', 50, 700.00,
        ARRAY['garden','studio','greenery','private'],
        ARRAY['/photos/garden1.jpg'],
        NULL,
        'Private garden studio surrounded by lush greenery, perfect for intimate outdoor shoots.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 14. Cinema Room – dark, equipped
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Private Cinema Screening Room', 'Marylebone', 40, 1000.00,
        ARRAY['cinema','dark','projection','surround sound'],
        ARRAY['/photos/cinema1.jpg'],
        NULL,
        'Fully equipped private cinema with 4K projector, surround sound, and blackout conditions.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 15. Rooftop Bar – outdoor, social
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Skyline Rooftop Bar', 'Southbank', 200, 2800.00,
        ARRAY['rooftop','bar','outdoor','city view'],
        ARRAY['/photos/rooftopbar1.jpg'],
        NULL,
        'Elevated rooftop bar with panoramic views of London landmarks and flexible seating arrangements.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 16. Traditional Pub – classic, characterful
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Historic Gastropub', 'Greenwich', 120, 900.00,
        ARRAY['pub','traditional','wooden beams','fireplace'],
        ARRAY['/photos/pub1.jpg'],
        NULL,
        'Characterful 400-year-old gastropub with original wooden beams, open fireplace, and charming nooks.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 17. Modern Kitchen – sleek, equipped
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Sleek Modern Kitchen', 'Canary Wharf', 80, 1500.00,
        ARRAY['kitchen','modern','stainless steel','minimalist'],
        ARRAY['/photos/modkitchen1.jpg'],
        NULL,
        'Ultra-modern kitchen with handleless cabinets, quartz worktops, and professional appliances.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 18. Photography Darkroom – specialized, dark
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Traditional Photography Darkroom', 'Soho', 25, 600.00,
        ARRAY['darkroom','photography','chemicals','red light'],
        ARRAY['/photos/darkroom1.jpg'],
        NULL,
        'Fully functional traditional wet-process darkroom with enlargers, trays, and safe red lighting.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 19. Industrial Hall – very large, raw
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Docklands Industrial Hall', 'Canary Wharf', 1200, 4000.00,
        ARRAY['industrial','hall','raw','loading bay'],
        ARRAY['/photos/indhall1.jpg','/photos/indhall2.jpg'],
        NULL,
        'Massive industrial hall with 12m ceilings, multiple loading bays, and raw unfinished aesthetic.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 20. Boutique Hotel Suite – luxurious, styled
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Luxury Boutique Hotel Suite', 'Westminster', 60, 2000.00,
        ARRAY['hotel suite','luxury','styled','designer'],
        ARRAY['/photos/hotelsuite1.jpg'],
        NULL,
        'Beautifully designed hotel suite in a historic building with designer furnishings and luxury amenities.',
        'ACTIVE', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- Add a few DRAFT venues to test filtering
-- 21. DRAFT Warehouse Conversion
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Future Warehouse Conversion', 'South London', 500, 1800.00,
        ARRAY['warehouse','conversion','planning','future'],
        ARRAY['/photos/future1.jpg'],
        NULL,
        'Planned warehouse conversion project - not yet ready for bookings.',
        'DRAFT', NOW(), NOW()) ON CONFLICT DO NOTHING;

-- 22. DRAFT Pop-up Space
INSERT INTO venues (owner_id, name, neighbourhood, size_m2, day_rate_eur, tags, photo_paths, clip_url, description, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Temporary Pop-up Space', 'Brixton', 30, 300.00,
        ARRAY['pop-up','temporary','experimental','short term'],
        ARRAY['/photos/temppop1.jpg'],
        NULL,
        'Experimental short-term pop-up concept still in development.',
        'DRAFT', NOW(), NOW()) ON CONFLICT DO NOTHING;