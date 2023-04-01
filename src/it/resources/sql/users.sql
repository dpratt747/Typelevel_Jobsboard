CREATE TABLE users (
    email TEXT NOT NULL,
    hashed_password TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    company TEXT,
    role TEXT NOT NULL
);

ALTER TABLE users ADD CONSTRAINT users_pkey PRIMARY KEY (email);