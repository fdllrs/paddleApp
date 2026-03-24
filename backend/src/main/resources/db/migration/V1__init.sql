-- 1. Initialize the Spatial Engine
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Define Strict Domain Boundaries (ENUMs)
CREATE TYPE match_status AS ENUM ('OPEN', 'FULL', 'CANCELLED', 'PLAYED');
CREATE TYPE wall_type AS ENUM ('CEMENT', 'GLASS');
CREATE TYPE floor_type AS ENUM ('CEMENT',  'SYNTHETIC_GRASS');
CREATE TYPE ticket_status AS ENUM ('SEARCHING',  'MATCHED', 'CANCELLED', 'EXPIRED');

-- 3. Independent Entities
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       display_name VARCHAR(50) NOT NULL,
                       division INT NOT NULL CHECK (division BETWEEN 1 AND 7),
                       created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE clubs (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name VARCHAR(100) NOT NULL,
                       address VARCHAR(255) NOT NULL,
                       neighborhood VARCHAR(50) NOT NULL,
                       coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
                       open_time TIME NOT NULL,
                       close_time TIME NOT NULL,
                       turn_duration_minutes INT NOT NULL DEFAULT 90,
                       created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Dependent Entities
CREATE TABLE courts (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                        name VARCHAR(100) NOT NULL,
                        is_covered BOOLEAN NOT NULL,
                        price_per_turn DECIMAL(10, 2) NOT NULL,
                        wall_type wall_type NOT NULL,
                        floor_type floor_type NOT NULL
);

CREATE TABLE matches (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         host_id UUID REFERENCES users(id) ON DELETE RESTRICT,
                         court_id UUID NOT NULL REFERENCES courts(id) ON DELETE RESTRICT,
                         status match_status NOT NULL DEFAULT 'OPEN',
                         start_date TIMESTAMPTZ NOT NULL,
                         end_date TIMESTAMPTZ NOT NULL,
                         target_division INT NOT NULL CHECK (target_division BETWEEN 1 AND 7),
                         duration_minutes INT NOT NULL DEFAULT 90,
                         price_per_person DECIMAL(10, 2) NOT NULL,
                         created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Concurrency Junctions & Queues
CREATE TABLE match_players (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                               player_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               joined_at TIMESTAMPTZ DEFAULT NOW(),
                               CONSTRAINT uk_match_player UNIQUE (match_id, player_id)
);

CREATE TABLE matchmaking_tickets (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     partner_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                     target_division INT NOT NULL CHECK (target_division BETWEEN 1 AND 7),
                                     search_location GEOGRAPHY(POINT, 4326) NOT NULL,
                                     max_radius_meters FLOAT NOT NULL,
                                     start_time TIMESTAMPTZ NOT NULL,
                                     end_time TIMESTAMPTZ NOT NULL,
                                     status ticket_status NOT NULL,
                                     matched_match_id UUID REFERENCES matches(id) ON DELETE SET NULL,
                                     preferred_court_id UUID NOT NULL REFERENCES courts(id) ON DELETE CASCADE ,
                                     preferred_club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                     preferred_match_date TIMESTAMPTZ NOT NULL,
                                     preferred_duration_minutes INT NOT NULL DEFAULT 90,
                                     created_at TIMESTAMPTZ DEFAULT NOW()

);

-- 6. Spatial Indexes
CREATE INDEX idx_clubs_coordinates ON clubs USING GIST (coordinates);
CREATE INDEX idx_tickets_search_location ON matchmaking_tickets USING GIST (search_location);