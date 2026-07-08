import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thread-safe logger for recording outcomes of database transactions to a log file.
 */
public class TransactionLogger {
    private static final String LOG_FILE = "transactions.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static synchronized void writeLog(String status, String txName, String details, Throwable err) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = DATE_FORMAT.format(new Date());
            pw.printf("[%s] [%s] Transaction: %s | Details: %s%n", timestamp, status, txName, details);
            if (err != null) {
                err.printStackTrace(pw);
            }
        } catch (IOException e) {
            System.err.println("TransactionLogger error: Failed to write to transaction log: " + e.getMessage());
        }
    }

    public static void logCommit(String txName, String details) {
        writeLog("COMMIT", txName, details, null);
    }

    public static void logRollback(String txName, String details, Throwable err) {
        writeLog("ROLLBACK", txName, details, err);
    }
}
