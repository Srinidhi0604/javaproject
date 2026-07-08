-- Seed Cities
INSERT INTO cities (name) VALUES ('Bengaluru');
INSERT INTO cities (name) VALUES ('Mumbai');
INSERT INTO cities (name) VALUES ('Chennai');

-- Seed Users
INSERT INTO users (username, password_hash, salt, role, wallet_balance, city) VALUES ('user123', '6b999f2e0f2b0e684bb04cb576e508fc3f831d680a51d6ef4a96eb106df6988a', 'staticsalt123', 'CUSTOMER', 100.0, 'Bengaluru');
INSERT INTO users (username, password_hash, salt, role, wallet_balance, city) VALUES ('admin', '849077332cf0e4d81fe15249ef6070abc14918baac53e3b4faf5a1760a3e99f3', 'staticsalt123', 'ADMIN', 0.0, 'Mumbai');
INSERT INTO users (username, password_hash, salt, role, wallet_balance, city) VALUES ('partner1', '62045e431ed97b22595db12108f67cfcda218822c995da5fdf08becf92dd43e0', 'staticsalt123', 'VENUE_PARTNER', 0.0, 'Bengaluru');
INSERT INTO users (username, password_hash, salt, role, wallet_balance, city) VALUES ('partner2', '62045e431ed97b22595db12108f67cfcda218822c995da5fdf08becf92dd43e0', 'staticsalt123', 'VENUE_PARTNER', 0.0, 'Mumbai');

-- Seed initial wallet ledger for user123 (id 1)
INSERT INTO wallet_ledger (user_id, amount, type) VALUES (1, 100.0, 'EARN');

-- Seed Venues
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Galaxy Cineplex', 'MOVIE_THEATRE', 1, 'Koramangala, Bengaluru', 3);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Sector 7 Arena', 'ARENA', 2, 'Andheri West, Mumbai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('The Gourmet Bistro', 'RESTAURANT', 1, 'Indiranagar, Bengaluru', 3);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Wonderland Activity Centre', 'ACTIVITY_CENTER', 3, 'ECR Highway, Chennai', 4);

-- Additional Venues
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Mumbai Cine Dome', 'MOVIE_THEATRE', 2, 'Bandra East, Mumbai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Ocean Breeze Dine', 'RESTAURANT', 2, 'Marine Drive, Mumbai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('The Craft Space', 'ACTIVITY_CENTER', 2, 'Colaba, Mumbai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Marina Screenings', 'MOVIE_THEATRE', 3, 'Adyar, Chennai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Dakshin Flavours', 'RESTAURANT', 3, 'T-Nagar, Chennai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Salty Waves Arena', 'ARENA', 3, 'Besant Nagar, Chennai', 4);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Playground Arena', 'ARENA', 1, 'Whitefield, Bengaluru', 3);
INSERT INTO venues (name, type, city_id, address, owner_user_id) VALUES ('Adventure Zone', 'ACTIVITY_CENTER', 1, 'Yelahanka, Bengaluru', 3);

-- Seed Catalog Items
-- 1. Movie (Standard)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price, is_special, format, surcharge) 
VALUES (1, 'MOVIE', 'Inception', 'A thief who steals corporate secrets through dream-sharing.', 'Sci-Fi', 148, 220.0, 0, '', 0.0);

-- 2. Movie (Special Screening)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price, is_special, format, surcharge) 
VALUES (1, 'MOVIE', 'IMAX: Dune 3', 'Paul Atreides seeks revenge against the conspirators.', 'Sci-Fi', 175, 250.0, 1, 'IMAX', 100.0);

-- 3. Dining
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (3, 'DINING', 'Chef''s Tasting Dinner', 'A curated 5-course Italian feast with mocktails.', 'Italian', 90, 50.0);

-- 4. Live Event
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (2, 'EVENT', 'Bandra Comedy Special', 'Hilarious evening featuring Mumbai''s funniest stand-up comics.', 'Comedy', 80, 350.0);

-- 5. Activity
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (4, 'ACTIVITY', 'Escape the Crypt Room', 'Solve puzzles to unlock the Egyptian crypt in 60 mins.', 'Adventure', 60, 300.0);

-- 6. Movie (Bengaluru)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price, is_special, format, surcharge) 
VALUES (1, 'MOVIE', 'Avatar: The Way of Water', 'Jake Sully lives with his newfound family formed on the extrasolar moon Pandora.', 'Action', 192, 240.0, 1, '3D', 60.0);

-- 7. Event (Bengaluru)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (11, 'EVENT', 'Coldplay Tribute Concert', 'A spectacular live tribute show playing all hits from Yellow to Higher Power.', 'Music', 120, 450.0);

-- 8. Activity (Bengaluru)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (12, 'ACTIVITY', 'Skydiving Simulation', 'Experience simulated free fall in a world-class vertical wind tunnel.', 'Adventure', 45, 600.0);

-- 9. Movie (Mumbai)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price, is_special, format, surcharge) 
VALUES (5, 'MOVIE', 'The Dark Knight', 'Batman raises the stakes in his war on crime with the help of Lt. Jim Gordon.', 'Action', 152, 200.0, 0, 'Standard', 0.0);

-- 10. Dining (Mumbai)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (6, 'DINING', 'Roof-top Candlelight Dinner', 'Indulge in a premium romantic continental candlelight table setting.', 'Continental', 120, 120.0);

-- 11. Activity (Mumbai)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (7, 'ACTIVITY', 'Art & Pottery Workshop', 'Learn to shape clay on the wheel and paint your pottery creation.', 'Crafts', 90, 150.0);

-- 12. Movie (Chennai)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price, is_special, format, surcharge) 
VALUES (8, 'MOVIE', 'Interstellar', 'A team of explorers travel through a wormhole in space in search of a new home.', 'Sci-Fi', 169, 210.0, 1, 'IMAX', 80.0);

-- 13. Dining (Chennai)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (9, 'DINING', 'Traditional South Indian Feast', 'Authentic unlimited feast served on traditional plantain leaves.', 'South Indian', 60, 40.0);

-- 14. Event (Chennai)
INSERT INTO catalog_items (venue_id, category, title, description, genre_or_cuisine, duration_minutes, base_price) 
VALUES (10, 'EVENT', 'Sunburn Arena Festival', 'High-energy electronic dance music event featuring world-class DJs.', 'Electronic', 180, 500.0);


-- Seed slots/showtimes_or_slots
-- Inception (1) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (1, '2:00 PM', 50, 50);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (1, '7:00 PM', 50, 50);

-- IMAX: Dune 3 (2) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (2, '1:00 PM', 30, 30);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (2, '7:30 PM', 30, 30);

-- Dining (3) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (3, '7:00 PM', 10, 10);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (3, '8:30 PM', 10, 10);

-- Event (4) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (4, '8:00 PM', 100, 100);

-- Activity (5) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (5, '11:00 AM', 20, 20);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (5, '3:00 PM', 20, 20);

-- Avatar 2 (6) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (6, '3:30 PM', 50, 50);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (6, '8:00 PM', 50, 50);

-- Coldplay Concert (7) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (7, '6:00 PM', 200, 200);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (7, '9:00 PM', 200, 200);

-- Skydiving (8) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (8, '10:00 AM', 15, 15);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (8, '2:00 PM', 15, 15);

-- The Dark Knight (9) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (9, '1:30 PM', 40, 40);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (9, '6:30 PM', 40, 40);

-- Candlelight Dinner (10) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (10, '7:30 PM', 12, 12);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (10, '9:30 PM', 12, 12);

-- Pottery Workshop (11) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (11, '11:00 AM', 15, 15);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (11, '4:00 PM', 15, 15);

-- Interstellar (12) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (12, '12:00 PM', 30, 30);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (12, '7:00 PM', 30, 30);

-- South Indian Feast (13) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (13, '12:30 PM', 25, 25);
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (13, '2:00 PM', 25, 25);

-- Sunburn Arena (14) Slots
INSERT INTO showtimes_or_slots (catalog_item_id, start_time, total_capacity, available_capacity) VALUES (14, '5:00 PM', 300, 300);


-- Seed Coupons
INSERT INTO coupons (code, discount_type, discount_value, max_redemptions, per_user_limit) VALUES ('WELCOME10', 'PERCENT', 10.0, 1000, 1);
INSERT INTO coupons (code, discount_type, discount_value, max_redemptions, per_user_limit) VALUES ('DISTRICT50', 'FLAT', 50.0, 500, 1);

-- Seed Pricing Rules for Inception evening show (slot 2)
INSERT INTO pricing_rules (catalog_item_id, day_of_week, time_range, multiplier) VALUES (1, 'Saturday', '19:00', 1.25);
INSERT INTO pricing_rules (catalog_item_id, day_of_week, time_range, multiplier) VALUES (1, 'Sunday', '19:00', 1.25);
