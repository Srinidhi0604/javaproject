import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles database schema migrations on startup by sequential execution of versioned .sql files.
 */
public class MigrationRunner {

    /**
     * Finds and applies all pending SQL schema migrations.
     */
    public static void runMigrations() throws SQLException {
        // 1. Initialize schema_version table
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection();
             Statement stmt = holder.getConnection().createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS schema_version (" +
                         "version INTEGER PRIMARY KEY," +
                         "script_name TEXT NOT NULL," +
                         "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }

        // 2. Fetch applied versions
        List<Integer> appliedVersions = new ArrayList<>();
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection();
             Statement stmt = holder.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version ORDER BY version ASC")) {
            while (rs.next()) {
                appliedVersions.add(rs.getInt("version"));
            }
        }

        // 3. Scan db/migration directory
        Path migrationDir = Paths.get("db/migration");
        if (!Files.exists(migrationDir)) {
            try {
                Files.createDirectories(migrationDir);
            } catch (IOException e) {
                throw new SQLException("Failed to create migration path: " + migrationDir, e);
            }
        }

        try {
            List<Path> migrationFiles = Files.list(migrationDir)
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                .collect(Collectors.toList());

            for (Path file : migrationFiles) {
                String filename = file.getFileName().toString();
                if (filename.startsWith("V") && filename.contains("__")) {
                    int underScoreIdx = filename.indexOf("__");
                    try {
                        int version = Integer.parseInt(filename.substring(1, underScoreIdx));
                        if (!appliedVersions.contains(version)) {
                            applyMigration(file, version, filename);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("[MigrationRunner] Skipped invalid migration naming: " + filename);
                    }
                }
            }
        } catch (IOException e) {
            throw new SQLException("Error checking schema migrations", e);
        }
    }

    private static void applyMigration(Path file, int version, String filename) throws SQLException {
        System.out.println("[MigrationRunner] Applying script: " + filename);
        
        String sql;
        try {
            sql = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Failed to read migration script: " + filename, e);
        }

        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            Connection conn = holder.getConnection();
            conn.setAutoCommit(false); // Enable transactional safety
            
            try (Statement stmt = conn.createStatement()) {
                // Strip comments line by line
                String cleanSql = java.util.Arrays.stream(sql.split("\r?\n"))
                    .map(line -> {
                        int commentIdx = line.indexOf("--");
                        if (commentIdx != -1) {
                            return line.substring(0, commentIdx);
                        }
                        return line;
                    })
                    .collect(Collectors.joining("\n"));

                // Split statements on semicolon
                String[] statements = cleanSql.split(";");
                for (String query : statements) {
                    String cleanQuery = query.trim();
                    if (!cleanQuery.isEmpty()) {
                        stmt.execute(cleanQuery);
                    }
                }
                
                // Write migration record
                String recordSql = "INSERT INTO schema_version (version, script_name) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(recordSql)) {
                    pstmt.setInt(1, version);
                    pstmt.setString(2, filename);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                System.out.println("[MigrationRunner] Version " + version + " applied successfully.");
            } catch (Exception e) {
                conn.rollback(); // Rollback complete version file on syntax/database error
                System.err.println("[MigrationRunner] Failure in transaction V" + version + ". Rollback initiated.");
                throw new SQLException("Migration transaction failed for script: " + filename, e);
            }
        }
    }
}
