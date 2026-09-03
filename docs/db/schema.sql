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

-- Módulo challenges: flag de productor verificado (mayor peso de voto).
ALTER TABLE producer_profiles ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    bpm INT NOT NULL,
    music_key VARCHAR(20),
    theme VARCHAR(255),
    deadline DATETIME NOT NULL,
    guest_artist_id BIGINT NOT NULL,
    prize_first VARCHAR(255),
    prize_second VARCHAR(255),
    prize_third VARCHAR(255),
    opportunity_pick_submission_id BIGINT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (guest_artist_id) REFERENCES users(id)
);

CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    producer_id BIGINT NOT NULL,
    audio_url VARCHAR(500) NOT NULL,
    submitted_at DATETIME NOT NULL,
    FOREIGN KEY (challenge_id) REFERENCES challenges(id),
    FOREIGN KEY (producer_id) REFERENCES users(id)
);

CREATE TABLE votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    voter_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(1000),
    UNIQUE (submission_id, voter_id),
    FOREIGN KEY (submission_id) REFERENCES submissions(id),
    FOREIGN KEY (voter_id) REFERENCES users(id)
);

-- rank es palabra reservada en MySQL, por eso la columna se llama rank_position.
CREATE TABLE challenge_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL UNIQUE,
    rank_position INT NOT NULL,
    points_awarded INT NOT NULL,
    badge VARCHAR(255),
    prize_text VARCHAR(255),
    FOREIGN KEY (challenge_id) REFERENCES challenges(id),
    FOREIGN KEY (submission_id) REFERENCES submissions(id)
);
