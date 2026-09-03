CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    city VARCHAR(255),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL
);

CREATE TABLE producer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    genres VARCHAR(255),
    bpm_min INT,
    bpm_max INT,
    experience_level VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE artist_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    genres VARCHAR(255),
    bio TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE beats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producer_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    bpm INT NOT NULL,
    music_key VARCHAR(20),
    audio_url VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (producer_id) REFERENCES users(id)
);
