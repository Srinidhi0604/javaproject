CREATE TABLE IF NOT EXISTS cities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT,
    role TEXT NOT NULL,
    wallet_balance DECIMAL(10,2) DEFAULT 0.00,
    city TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS venues (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL, -- MOVIE_THEATRE, RESTAURANT, ARENA, ACTIVITY_CENTER
    city_id INTEGER REFERENCES cities(id),
    address TEXT,
    owner_user_id INTEGER REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS catalog_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venue_id INTEGER REFERENCES venues(id),
    category TEXT NOT NULL, -- MOVIE, DINING, EVENT, PLAY, ACTIVITY
    title TEXT NOT NULL,
    description TEXT,
    genre_or_cuisine TEXT,
    duration_minutes INTEGER,
    base_price DECIMAL(10,2),
    rating_avg REAL DEFAULT 0.0,
    is_special BOOLEAN DEFAULT 0,
    format TEXT DEFAULT '',
    surcharge DECIMAL(10,2) DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS showtimes_or_slots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    catalog_item_id INTEGER REFERENCES catalog_items(id),
    start_time TEXT NOT NULL,
    total_capacity INTEGER NOT NULL,
    available_capacity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
    id TEXT PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    catalog_item_id INTEGER REFERENCES catalog_items(id),
    slot_id INTEGER REFERENCES showtimes_or_slots(id),
    quantity INTEGER NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    status TEXT NOT NULL, -- PENDING, CONFIRMED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_seats (
    booking_id TEXT REFERENCES bookings(id),
    seat_label TEXT NOT NULL,
    PRIMARY KEY (booking_id, seat_label)
);

CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id TEXT REFERENCES bookings(id),
    amount DECIMAL(10,2) NOT NULL,
    status TEXT NOT NULL, -- PENDING, SUCCESS, FAILED, REFUNDED
    method TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT UNIQUE NOT NULL,
    discount_type TEXT NOT NULL, -- PERCENT, FLAT
    discount_value DECIMAL(10,2) NOT NULL,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    max_redemptions INTEGER,
    per_user_limit INTEGER
);

CREATE TABLE IF NOT EXISTS coupon_redemptions (
    coupon_id INTEGER REFERENCES coupons(id),
    user_id INTEGER REFERENCES users(id),
    booking_id TEXT REFERENCES bookings(id),
    redeemed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (coupon_id, user_id, booking_id)
);

CREATE TABLE IF NOT EXISTS wallet_ledger (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    type TEXT NOT NULL, -- EARN, BURN
    reference_booking_id TEXT REFERENCES bookings(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reviews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    catalog_item_id INTEGER REFERENCES catalog_items(id),
    booking_id TEXT REFERENCES bookings(id),
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    booking_id TEXT REFERENCES bookings(id),
    message TEXT NOT NULL,
    send_at TIMESTAMP NOT NULL,
    sent BOOLEAN DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pricing_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    catalog_item_id INTEGER REFERENCES catalog_items(id),
    day_of_week TEXT,
    time_range TEXT,
    multiplier REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS wishlist (
    user_id INTEGER REFERENCES users(id),
    catalog_item_id INTEGER REFERENCES catalog_items(id),
    PRIMARY KEY (user_id, catalog_item_id)
);
