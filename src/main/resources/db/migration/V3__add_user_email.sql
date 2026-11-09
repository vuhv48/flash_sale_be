ALTER TABLE users
    ADD COLUMN email VARCHAR(320);

ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);
