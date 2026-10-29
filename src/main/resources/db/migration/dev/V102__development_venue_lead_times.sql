UPDATE venues SET minimum_lead_time_minutes = CASE name
    WHEN 'Westergasfabriek Studio A' THEN 2880
    WHEN 'Jordaan Loft' THEN 720
    WHEN 'NDSM Raw Space' THEN 4320
    WHEN 'De Pijp Atelier' THEN 720
    WHEN 'Oud-West Kitchen Studio' THEN 1440
    WHEN 'Zuidas Glass Tower Floor 22' THEN 2880
    WHEN 'Amsterdam-Noord Warehouse' THEN 4320
    WHEN 'Plantage Garden Studio' THEN 1440
    WHEN 'Harbour View Penthouse' THEN 2880
    WHEN 'Vondelpark Townhouse' THEN 1440
    ELSE minimum_lead_time_minutes
END
WHERE status = 'ACTIVE';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM venues WHERE status = 'ACTIVE' AND minimum_lead_time_minutes IS NULL) THEN
        RAISE EXCEPTION 'Every ACTIVE development venue must have minimum_lead_time_minutes';
    END IF;
END $$;
