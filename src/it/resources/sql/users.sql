CREATE TABLE users (
    email TEXT PRIMARY KEY,
    hashed_password TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    company TEXT,
    role TEXT NOT NULL
);
