CREATE DATABASE board;

\c board;

CREATE TABLE jobs (
    id UUID DEFAULT gen_random_uuid(),
    date BIGINT NOT NULL,
    owner_email TEXT NOT NULL,
    company TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    external_url TEXT NOT NULL,
    remote BOOLEAN NOT NULL DEFAULT false,
    location TEXT,
    currency TEXT,
    salary_lo INTEGER,
    salary_hi INTEGER,
    country TEXT,
    tags TEXT[],
    image TEXT,
    seniority TEXT,
    other TEXT,
    active BOOLEAN NOT NULL DEFAULT true
);

ALTER TABLE jobs ADD CONSTRAINT jobs_pkey PRIMARY KEY (id);

CREATE TABLE users (
    email TEXT PRIMARY KEY,
    hashed_password TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    company TEXT,
    role TEXT NOT NULL
);

CREATE TABLE recovery_tokens (
    email TEXT PRIMARY KEY,
    token TEXT NOT NULL,
    expiration BIGINT NOT NULL
);
