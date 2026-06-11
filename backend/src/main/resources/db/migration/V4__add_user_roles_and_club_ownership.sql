-- Create User Role enum and add role to users table
CREATE TYPE user_role AS ENUM ('PLAYER', 'CLUB_ADMIN');
ALTER TABLE users ADD COLUMN role user_role NOT NULL DEFAULT 'PLAYER';

-- Associate a Club Manager to a Club
ALTER TABLE clubs ADD COLUMN manager_id UUID REFERENCES users(id) ON DELETE SET NULL;
