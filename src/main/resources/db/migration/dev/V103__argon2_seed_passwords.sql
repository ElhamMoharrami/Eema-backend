-- Development-only seed credentials; preserve applied V100 checksum and all user/venue FKs.
-- Only the two immutable seed identities are updated. Password remains password123.
UPDATE users SET password_hash='$argon2id$v=19$m=65536,t=3,p=1$S0pV2GrzC2BXROx9wyJuPQ$wwkJavOLA69Tbdsh/B7Y8z3Qa3COnJ94zVwOBJhGtXo' WHERE id='00000000-0000-0000-0000-000000000001' AND email='owner@eema-dev.local';
UPDATE users SET password_hash='$argon2id$v=19$m=65536,t=3,p=1$aILaP7nm7QYL/91frPKRVg$MUB/teZbX3lF6pIc1iIdbeykZ2O+7rzXaznR8h7/iw8' WHERE id='00000000-0000-0000-0000-000000000002' AND email='scout@eema-dev.local';
