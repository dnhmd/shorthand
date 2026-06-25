-- V1: Initial Links Table Structure | Public Schema | Backend Service
CREATE TABLE links (
    id BIGINT PRIMARY KEY,
    code VARCHAR(15) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);