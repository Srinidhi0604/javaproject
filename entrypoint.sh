#!/bin/sh
# entrypoint.sh — run on container start
# If the SQLite DB doesn't exist at the persistent mount, copy the seed DB
DB_PATH="${DATABASE_URL:-jdbc:sqlite:district.db}"
# Strip the jdbc:sqlite: prefix to get the raw path
RAW_PATH="${DB_PATH#jdbc:sqlite:}"

if [ ! -f "$RAW_PATH" ]; then
    echo "[entrypoint] No database found at $RAW_PATH — copying seed database..."
    cp /app/district_seed.db "$RAW_PATH" 2>/dev/null || echo "[entrypoint] No seed DB bundled; MigrationRunner will create schema."
fi

exec java -cp "bin:lib/*" BookingSystem
