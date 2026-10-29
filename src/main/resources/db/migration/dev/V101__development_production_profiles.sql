UPDATE venues SET
    max_crew_size = 45, permitted_activities = ARRAY['PHOTO','FILM','COMMERCIAL']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = TRUE, ceiling_height_m = 6.50, loading_access = TRUE,
    parking_available = TRUE, parking_spaces = 8, floor_number = 0, lift_available = FALSE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = FALSE,
    production_rules = 'Protect original surfaces; haze requires advance approval.', power_details = 'Three-phase 63A and multiple 16A circuits.'
WHERE name = 'Westergasfabriek Studio A';

UPDATE venues SET
    max_crew_size = 12, permitted_activities = ARRAY['PHOTO','INTERVIEW','CONTENT_CREATION']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = FALSE, ceiling_height_m = 3.10, loading_access = FALSE,
    parking_available = FALSE, parking_spaces = 0, floor_number = 3, lift_available = FALSE,
    wheelchair_accessible = FALSE, bathrooms_available = TRUE, sound_recording_suitable = TRUE,
    production_rules = 'Quiet productions only; shoes off in styled areas.', power_details = 'Standard domestic 230V circuits.'
WHERE name = 'Jordaan Loft';

UPDATE venues SET
    max_crew_size = 100, permitted_activities = ARRAY['PHOTO','FILM','VIDEO','COMMERCIAL','MUSIC_VIDEO']::production_activity[],
    prohibited_activities = ARRAY[]::production_activity[], natural_light = TRUE,
    blackout_possible = TRUE, ceiling_height_m = 9.00, loading_access = TRUE,
    parking_available = TRUE, parking_spaces = 20, floor_number = 0, lift_available = FALSE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = FALSE,
    production_rules = 'Rigging and atmospheric effects require a method statement.', power_details = 'Three-phase 125A production supply.'
WHERE name = 'NDSM Raw Space';

UPDATE venues SET
    max_crew_size = 10, permitted_activities = ARRAY['PHOTO','INTERVIEW','CONTENT_CREATION']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = TRUE, ceiling_height_m = 3.40, loading_access = FALSE,
    parking_available = FALSE, parking_spaces = 0, floor_number = 2, lift_available = TRUE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = TRUE,
    production_rules = 'Keep shared stairwell clear.', power_details = '230V outlets on two separate circuits.'
WHERE name = 'De Pijp Atelier';

UPDATE venues SET
    max_crew_size = 14, permitted_activities = ARRAY['PHOTO','VIDEO','COMMERCIAL','CONTENT_CREATION']::production_activity[],
    prohibited_activities = ARRAY[]::production_activity[], natural_light = TRUE,
    blackout_possible = FALSE, ceiling_height_m = 3.20, loading_access = TRUE,
    parking_available = FALSE, parking_spaces = 0, floor_number = 0, lift_available = FALSE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = TRUE,
    production_rules = 'Food styling and normal kitchen use are permitted; open flame by agreement.', power_details = 'Dedicated appliance circuits and standard 230V outlets.'
WHERE name = 'Oud-West Kitchen Studio';

UPDATE venues SET
    max_crew_size = 20, permitted_activities = ARRAY['PHOTO','VIDEO','INTERVIEW','COMMERCIAL','CONTENT_CREATION']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = FALSE, ceiling_height_m = 2.90, loading_access = TRUE,
    parking_available = TRUE, parking_spaces = 4, floor_number = 22, lift_available = TRUE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = TRUE,
    production_rules = 'Building security requires a crew list 24 hours ahead.', power_details = 'Standard 230V office power.'
WHERE name = 'Zuidas Glass Tower Floor 22';

UPDATE venues SET
    max_crew_size = 70, permitted_activities = ARRAY['PHOTO','FILM','VIDEO','COMMERCIAL','MUSIC_VIDEO']::production_activity[],
    prohibited_activities = ARRAY[]::production_activity[], natural_light = TRUE,
    blackout_possible = TRUE, ceiling_height_m = 7.00, loading_access = TRUE,
    parking_available = TRUE, parking_spaces = 15, floor_number = 0, lift_available = FALSE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = FALSE,
    production_rules = 'Vehicles may enter only through the marked loading bay.', power_details = 'Three-phase 63A near the shooting floor.'
WHERE name = 'Amsterdam-Noord Warehouse';

UPDATE venues SET
    max_crew_size = 8, permitted_activities = ARRAY['PHOTO','INTERVIEW','CONTENT_CREATION']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = FALSE, ceiling_height_m = 2.80, loading_access = FALSE,
    parking_available = FALSE, parking_spaces = 0, floor_number = 0, lift_available = FALSE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = TRUE,
    production_rules = 'Garden planting must not be moved or altered.', power_details = 'Outdoor-rated 230V outlet available.'
WHERE name = 'Plantage Garden Studio';

UPDATE venues SET
    max_crew_size = 18, permitted_activities = ARRAY['PHOTO','VIDEO','INTERVIEW','COMMERCIAL','CONTENT_CREATION']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = FALSE, ceiling_height_m = 3.00, loading_access = TRUE,
    parking_available = TRUE, parking_spaces = 2, floor_number = 8, lift_available = TRUE,
    wheelchair_accessible = TRUE, bathrooms_available = TRUE, sound_recording_suitable = FALSE,
    production_rules = 'Terrace use is weather-dependent; no unsecured equipment outside.', power_details = 'Standard 230V domestic circuits.'
WHERE name = 'Harbour View Penthouse';

UPDATE venues SET
    max_crew_size = 16, permitted_activities = ARRAY['PHOTO','FILM','INTERVIEW','COMMERCIAL']::production_activity[],
    prohibited_activities = ARRAY['MUSIC_VIDEO']::production_activity[], natural_light = TRUE,
    blackout_possible = TRUE, ceiling_height_m = 3.30, loading_access = FALSE,
    parking_available = TRUE, parking_spaces = 1, floor_number = 0, lift_available = FALSE,
    wheelchair_accessible = FALSE, bathrooms_available = TRUE, sound_recording_suitable = TRUE,
    production_rules = 'Furniture may be moved only with the location manager present.', power_details = 'Standard 230V domestic supply.'
WHERE name = 'Vondelpark Townhouse';

UPDATE venues
SET photo_paths = ARRAY['/api/upload/files/' || id::text || '.jpg']
WHERE status = 'ACTIVE' AND cardinality(photo_paths) = 0;
