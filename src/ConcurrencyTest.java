import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Concurrency Load Test class.
 * Simulates multiple concurrent booking requests for the last remaining seat.
 * Verifies that exactly one request succeeds and all others are rejected or fail, preventing overbooking.
 */
public class ConcurrencyTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CONCURRENCY LOAD TEST: PREVENT OVERBOOKING     ");
        System.out.println("==================================================");

        // 1. Boot up migrations to initialize clean database
        try {
            System.out.println("[Test] Running database migrations...");
            MigrationRunner.runMigrations();
            System.out.println("[Test] Database migrations completed.");
        } catch (Exception e) {
            System.err.println("[Test] Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 2. Set slot ID 1 (e.g., Inception Bengaluru slot) to have exactly 1 seat capacity
        int targetSlotId = 1;
        try (ConnectionHolder holder = DatabaseManager.acquireWriteConnection()) {
            String updateSql = "UPDATE showtimes_or_slots SET total_capacity = 1, available_capacity = 1 WHERE id = ?";
            try (PreparedStatement pstmt = holder.getConnection().prepareStatement(updateSql)) {
                pstmt.setInt(1, targetSlotId);
                pstmt.executeUpdate();
            }
            System.out.println("[Test] Initialized Slot ID " + targetSlotId + " with exactly 1 available seat.");
        } catch (SQLException e) {
            System.err.println("[Test] Failed to prepare slot capacity: " + e.getMessage());
            System.exit(1);
        }

        // 3. Setup concurrent executors (10 threads concurrently trying to book)
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            tasks.add(() -> {
                String bookingId = "TX" + index + "_" + UUID.randomUUID().toString().substring(0, 8);
                try {
                    // Try booking 1 seat for catalog item 1 (Inception), slot ID 1
                    BookingDAO.createBookingTransaction(
                        bookingId,
                        1, // User ID
                        1, // Catalog Item ID
                        targetSlotId,
                        1, // Quantity = 1 seat
                        new BigDecimal("150.00"), // Cost
                        new String[]{"A1"}, // Seat labels
                        "", // Coupon code
                        false, // Redeem wallet
                        BigDecimal.ZERO // Wallet burn
                    );
                    System.out.println("[Thread " + index + "] Booking " + bookingId + " SUCCESSFUL!");
                    return true;
                } catch (CapacityExceededException e) {
                    System.out.println("[Thread " + index + "] Booking REJECTED: Capacity Exceeded.");
                    return false;
                } catch (Exception e) {
                    System.out.println("[Thread " + index + "] Booking FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    return false;
                }
            });
        }

        // Run tasks concurrently
        int successCount = 0;
        int failureCount = 0;
        try {
            System.out.println("[Test] Dispatching " + threadCount + " concurrent booking requests...");
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("[Test] Executor execution error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        System.out.println("==================================================");
        System.out.println("              CONCURRENCY RESULTS                 ");
        System.out.println("==================================================");
        System.out.println("Total Requests Sent : " + threadCount);
        System.out.println("Success Bookings    : " + successCount);
        System.out.println("Rejected/Failed     : " + failureCount);
        System.out.println("==================================================");

        // 4. Double check the final capacity in database
        try (ConnectionHolder holder = DatabaseManager.acquireReadConnection()) {
            String querySql = "SELECT available_capacity FROM showtimes_or_slots WHERE id = ?";
            try (PreparedStatement pstmt = holder.getConnection().prepareStatement(querySql)) {
                pstmt.setInt(1, targetSlotId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int finalCap = rs.getInt("available_capacity");
                        System.out.println("Final Database Available Capacity: " + finalCap);
                        if (successCount == 1 && finalCap == 0) {
                            System.out.println(">>> SUCCESS: Concurrency check PASSED. Exactly 1 booking succeeded. Zero overbooking! <<<");
                        } else {
                            System.out.println(">>> FAILURE: Concurrency check FAILED. Overbooking occurred or zero success! <<<");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Test] Final validation query failed: " + e.getMessage());
        }
    }
}
