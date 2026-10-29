-- Forward-only repair for the two local development accounts seeded by V8.
-- Password: password123 (LOCAL DEVELOPMENT ONLY)
UPDATE users
SET password_hash = '$2b$10$QIGWfaWROypTPzZCaFYn5.ot.eR.tThhgL3sPmBbY2XFO4f9Z28qG'
WHERE id IN ('00000000-0000-0000-0000-000000000001',
             '00000000-0000-0000-0000-000000000002');
