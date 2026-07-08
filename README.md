# bookMyDistrict — Unified Booking Super-App

An advanced, framework-free Java web application modeled after District by Zomato, covering movies, dining, live events, and activities. The application features a robust Object-Oriented Java backend with **SQLite JDBC persistence** as the sole source of truth and a premium, responsive front-end dashboard.

---

## 🏛️ System Architecture

```
+-------------------------------------------------------------+
|                Web Frontend (index.html)                    |
|  - Zomato District branding (Dark theme & Glassmorphism)    |
|  - Real-time location filters & category tabs               |
|  - Interactive seat grids, guest sizes, & tier selectors    |
|  - Dynamic Wallet ledgers & coupon redemptions              |
+-------------------------------------------------------------+
                              |
                      JSON HTTP Requests
                              |
                              v
+-------------------------------------------------------------+
|                 Java Backend REST API                       |
|  - Built-in com.sun.net.httpserver.HttpServer (Port 8080)   |
|  - Multi-category route handlers with SQLite transactions   |
|  - Real-time background notifications polling daemon        |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                 JDBC Persistence Layer                      |
|  - SQLite (district.db) is the single source of truth       |
|  - Zero in-memory state tracking for active registries       |
|  - SQL Row-level authorization filters                       |
+-------------------------------------------------------------+
```

---

## 📂 Project Directory Structure

```
quick-hawking/
├── district.db                        # SQLite Relational Database file (auto-generated)
├── lib/
│   └── sqlite-jdbc-3.36.0.3.jar       # SQLite JDBC database driver JAR
├── bin/                               # Target directory for compiled class files
├── README.md                          # Comprehensive documentation (this file)
└── src/
    ├── Bookable.java                  # Seating booking transaction contract
    ├── Displayable.java               # Info presentation contract
    ├── DatabaseManager.java           # JDBC SQLite connection provider factory
    ├── Venue.java                     # Abstraction for physical hosting spaces
    ├── Entertainment.java             # Base abstract model for catalog items
    ├── Movie.java                     # Movie model with seat mapping
    ├── SpecialScreening.java          # IMAX/4DX specialized subclass of Movie
    ├── DiningOffer.java               # Restaurant reservation model
    ├── LiveEvent.java                 # Ticketed concerts with tiered pricing
    ├── ActivityExperience.java        # Capacity slot-based activities
    ├── Booking.java                   # Transaction receipt model
    ├── BookingSystem.java             # HTTP Server, REST routers & poller thread
    ├── InvalidSeatException.java      # Checked exception (capacity/boundary overflow)
    ├── BookingNotFoundException.java  # Checked exception (unregistered booking ID)
    ├── ShowtimeNotFoundException.java # Checked exception (invalid slots)
    ├── AlreadyCancelledException.java # Checked exception (double cancellation)
    └── index.html                     # Responsive Single Page Application (SPA)
```

---

## ☕ Core Java & OOP Concepts Implemented

### 1. Relational Persistence via JDBC
- **Zero-Framework Architecture**: No Spring, JPA, or Hibernate is used. All read, write, update, and delete actions are executed using pure **JDBC Parameterized Queries** to prevent SQL injection.
- **Transactional Atomicity**: Reservations check capacities, apply coupons, deduct loyalty wallets, and create billing logs under single, synchronized SQLite database transactions (`Connection.setAutoCommit(false)`), rollback on failure.
- **Row-Level Security**: The `/api/bookings` search logic filters results using SQL row constraints (`WHERE owner_id = ?` or `WHERE user_id = ?`) to secure transaction data between customer, venue partner, and administrator accounts.

### 2. Deep Object-Oriented Design (OOD)
- **Unified Abstractions**: The abstract class [Entertainment](file:///src/Entertainment.java) serves as the foundation for all category listings. Subclasses like `Movie`, `DiningOffer`, `LiveEvent`, and `ActivityExperience` inherit core fields but implement category-specific logic.
- **Interfaces (`Bookable`, `Displayable`)**: [Movie](file:///src/Movie.java) implements `Bookable` to enforce booking behavior, while all catalog subclasses implement `Displayable` to output metadata details.
- **Constructor Overloading & Super Calls**: Subclasses leverage constructor chaining (`super(...)`) to inherit and initialize basic properties while instantiating unique parameters (e.g. `surcharge` in `SpecialScreening`).

### 3. Polymorphism & Specializations
- **Method Overriding**: `SpecialScreening` overrides `getBasePrice()` to dynamically inject surcharges onto standard tickets. `Movie`, `DiningOffer`, and others override `toJson()` to build custom serialized structures.
- **Method Overloading**: `Movie` overloads `book(customerName, seats)` with a showtime-specific booking method `book(customerName, seats, showtime)` to reserve specific slots.

### 4. Concurrency & Background Threads
- **Notification Daemon Thread**: A dedicated poller thread runs in the background of [BookingSystem](file:///src/BookingSystem.java), checking the `notifications` table periodically to simulate ticket reminders at the venue without locking main request channels.

### 5. Custom Exception-to-JSON Mapping
- Four custom checked exceptions (`InvalidSeatException`, `BookingNotFoundException`, `ShowtimeNotFoundException`, `AlreadyCancelledException`) propagate from database updates up to the server handlers where they are formatted into standard JSON HTTP error payloads.

---

## 📡 REST API Reference

### 1. Catalog & Recommendations
- `GET /api/catalog?city={city}&category={cat}&genre={gen}&search={text}&user_id={id}`: Returns filtered items, average ratings, and wishlist flags.
- `GET /api/catalog/recommended?user_id={id}`: Returns recommended items matching category frequencies from user's booking history.
- `GET /api/catalog/trending`: Returns top catalog items based on booking count over the last 7 days.

### 2. Transaction Services
- `POST /api/book`: Evaluates pricing rules, surge rates, wallet burn flags, coupon limits, and inserts a pending booking.
- `POST /api/payment/confirm`: Confirms a pending payment, updates states, grants 10% loyalty cashback points, and schedules reminders.
- `POST /api/cancel`: Cancels booking, restores capacities, and rolls back loyalty transactions.

### 3. Account & Engagement Features
- `POST /api/login` & `POST /api/register`: Session and signup services (new customers receive 100 points).
- `GET /api/wallet/history?user_id={id}`: Fetches earn-and-burn wallet ledger history.
- `POST /api/reviews/add` & `GET /api/reviews?catalog_item_id={id}`: Processes user comments and average ratings.
- `POST /api/wishlist/toggle` & `GET /api/wishlist?user_id={id}`: Manages favorite feeds.
- `GET /api/notifications?user_id={id}`: Retrieves event reminders.

---

## 🚀 Setup & Compile Instructions

### 1. Compile the Source Code
Compile the Java classes with the library driver classpath:
```powershell
New-Item -ItemType Directory -Force -Path bin
javac -cp "lib/*" -d bin src/*.java
```

### 2. Launch the Application Server
Run the BookingSystem with the binary output and dependency classes:
```powershell
java -cp "bin;lib/*" BookingSystem
```

### 3. Open the Dashboard
Open your browser and navigate to:
👉 **[http://localhost:8080/](http://localhost:8080/)**
