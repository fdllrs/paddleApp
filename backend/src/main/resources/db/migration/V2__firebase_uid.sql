ALTER TABLE users
    ADD firebase_uid VARCHAR(28);

ALTER TABLE users
    ALTER COLUMN firebase_uid SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uc_users_firebase_uid UNIQUE (firebase_uid);
