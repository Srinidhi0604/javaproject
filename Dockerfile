# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy source and libraries
COPY src/       ./src/
COPY lib/       ./lib/

# Compile all Java sources into bin/
RUN mkdir -p bin && \
    javac -cp "lib/*" -d bin src/*.java

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy compiled classes, libs, migrations, and config
COPY --from=build /app/bin/       ./bin/
COPY --from=build /app/lib/       ./lib/
COPY db/                          ./db/
COPY config.properties            ./config.properties
COPY entrypoint.sh                ./entrypoint.sh

# Bundle the seed database (entrypoint copies it to persistent volume on first run)
COPY district.db                  ./district_seed.db

# Make entrypoint executable
RUN chmod +x entrypoint.sh

# Data directory for SQLite persistent volume
# On Railway/Render, mount a volume at /data and set DATABASE_URL=jdbc:sqlite:/data/district.db
RUN mkdir -p /data

# Expose default port (cloud platforms override with $PORT env var)
EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh"]
