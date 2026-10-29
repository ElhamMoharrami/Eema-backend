-- Development only. Password for both accounts: password123
INSERT INTO users (id, email, password_hash, role, full_name, email_verified) VALUES
('00000000-0000-0000-0000-000000000001', 'owner@eema-dev.local', '$2b$10$QIGWfaWROypTPzZCaFYn5.ot.eR.tThhgL3sPmBbY2XFO4f9Z28qG', 'OWNER', 'Dev Owner', TRUE),
('00000000-0000-0000-0000-000000000002', 'scout@eema-dev.local', '$2b$10$QIGWfaWROypTPzZCaFYn5.ot.eR.tThhgL3sPmBbY2XFO4f9Z28qG', 'SCOUT', 'Dev Scout', TRUE);

INSERT INTO venues (owner_id, name, neighbourhood, size_m2, hourly_rate_eur,
                    minimum_booking_minutes, full_day_rate_eur, tags, description, status) VALUES
('00000000-0000-0000-0000-000000000001', 'Westergasfabriek Studio A', 'Westerpark', 280, 225.00, 240, 1800.00, ARRAY['industrial','daylight','high-ceiling','parking'], 'Raw industrial studio with north-facing skylights.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Jordaan Loft', 'Jordaan', 120, 120.00, 240, 950.00, ARRAY['loft','daylight','canal-view','quiet'], 'Top-floor loft with original wooden beams and canal views.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'NDSM Raw Space', 'NDSM', 600, 300.00, 240, 2400.00, ARRAY['industrial','raw','large','high-ceiling','outside-area'], 'Shipyard hall with exposed steel trusses and loading access.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'De Pijp Atelier', 'De Pijp', 85, 85.00, 180, 680.00, ARRAY['atelier','daylight','white-walls','quiet'], 'Classic Amsterdam atelier with south-facing windows.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Oud-West Kitchen Studio', 'Oud-West', 95, 95.00, 240, 750.00, ARRAY['kitchen','daylight','marble','styled'], 'Purpose-built kitchen studio with professional equipment.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Zuidas Glass Tower Floor 22', 'Zuidas', 180, 175.00, 240, 1400.00, ARRAY['corporate','panoramic-view','modern','city-view'], 'Corner office floor with panoramic city views.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Amsterdam-Noord Warehouse', 'Noord', 450, 265.00, 240, 2100.00, ARRAY['warehouse','industrial','raw','loading-dock','parking'], 'Post-war warehouse with loading bays and 7m clearance.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Plantage Garden Studio', 'Plantage', 65, 75.00, 180, NULL, ARRAY['garden','daylight','greenery','quiet','outdoor'], 'Ground-floor studio with private garden.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Harbour View Penthouse', 'Oostelijk Havengebied', 200, 200.00, 240, 1600.00, ARRAY['penthouse','harbour-view','modern','daylight','rooftop'], 'Contemporary penthouse with wraparound terrace.', 'ACTIVE'),
('00000000-0000-0000-0000-000000000001', 'Vondelpark Townhouse', 'Oud-Zuid', 160, 150.00, 240, 1200.00, ARRAY['residential','classic','daylight','styled','quiet'], 'Styled townhouse near Vondelpark.', 'ACTIVE');
