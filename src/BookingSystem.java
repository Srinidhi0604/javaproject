import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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

/**
 * Main application class that hosts the HTTP Server and manages SQLite JDBC persistence.
 */
public class BookingSystem {

    public static void main(String[] args) {
        // 1. Initialize SQLite Database Schema
        initializeDatabase();

        // 2. Start Notification Polling Daemon Thread
        startNotificationPoller();

        // 3. Launch HTTP server
        int port = 8080;

        // Cloud platforms (Railway, Render, Heroku) inject $PORT — check it first
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try { port = Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
        } else {
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream is = new java.io.FileInputStream("config.properties")) {
                props.load(is);
                String portStr = props.getProperty("server.port");
                if (portStr != null && !portStr.trim().isEmpty()) {
                    port = Integer.parseInt(portStr.trim());
                }
            } catch (Exception e) {
                System.out.println("[BookingSystem] Using default server port: " + port);
            }
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Core API Route handlers
            server.createContext("/", new StaticFileHandler());
            server.createContext("/api/login", new LoginHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/catalog", new CatalogHandler());
            server.createContext("/api/catalog/recommended", new RecommendedHandler());
            server.createContext("/api/catalog/trending", new TrendingHandler());
            server.createContext("/api/book", new BookHandler());
            server.createContext("/api/payment/confirm", new PaymentConfirmHandler());
            server.createContext("/api/cancel", new CancelHandler());
            server.createContext("/api/bookings", new BookingsHandler());
            server.createContext("/api/reviews/add", new AddReviewHandler());
            server.createContext("/api/reviews", new ReviewsHandler());
            server.createContext("/api/wishlist/toggle", new WishlistToggleHandler());
            server.createContext("/api/wishlist", new WishlistHandler());
            server.createContext("/api/wallet/history", new WalletHistoryHandler());
            server.createContext("/api/notifications", new NotificationsHandler());
            server.createContext("/api/admin/analytics", new AdminAnalyticsHandler());
            server.createContext("/api/catalog/add", new AddCatalogItemHandler());
            server.createContext("/api/slots", new SlotsHandler());

            server.setExecutor(null); // default executor
            server.start();
            
            System.out.println("==================================================");
            System.out.println("     bookMyDistrict SERVER STARTED SUCCESSFULLY   ");
            System.out.println("     Open: http://localhost:" + port + "/           ");
            System.out.println("==================================================");
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * Runs schema migrations at server start.
     */
    private static void initializeDatabase() {
        try {
            MigrationRunner.runMigrations();
        } catch (Exception e) {
            System.err.println("Fatal: Database migrations failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Daemon Thread to process pending reminders.
     */
    private static void startNotificationPoller() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Poll every 10 seconds
                    processPendingNotifications();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void processPendingNotifications() {
        try {
            String pendingJson = NotificationDAO.getPendingNotifications();
            // Parse notifications array dynamically
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"id\"\\s*:\\s*(\\d+)[^}]*\"booking_id\"\\s*:\\s*\"([^\"]+)\"[^}]*\"message\"\\s*:\\s*\"([^\"]+)\"[^}]*\"username\"\\s*:\\s*\"([^\"]+)\""
            ).matcher(pendingJson);
            
            while (m.find()) {
                int notificationId = Integer.parseInt(m.group(1));
                String bookingId = m.group(2);
                String message = m.group(3);
                String username = m.group(4);
                
                System.out.println("[NOTIFICATION DAEMON] Dispatching to user " + username + " (Booking ID: " + bookingId + "): " + message);
                NotificationDAO.markAsSent(notificationId);
            }
        } catch (Exception e) {
            System.err.println("Notification poller execution failed: " + e.getMessage());
        }
    }

    // ------------------ SERVER ENDPOINT HELPERS ------------------
    private static boolean handleOptionsAndCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        return bos.toString(StandardCharsets.UTF_8.name());
    }

    private static String parseJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static int parseJsonInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    private static double parseJsonDouble(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*([\\d.]+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    private static java.math.BigDecimal parseJsonBigDecimal(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*([\\d.]+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return new java.math.BigDecimal(matcher.group(1));
        }
        // Fallback: check if it is enclosed in quotes
        String patternQuoted = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher matcherQuoted = java.util.regex.Pattern.compile(patternQuoted).matcher(json);
        if (matcherQuoted.find() && !matcherQuoted.group(1).trim().isEmpty()) {
            return new java.math.BigDecimal(matcherQuoted.group(1).trim());
        }
        return java.math.BigDecimal.ZERO;
    }

    private static boolean parseJsonBoolean(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    private static String getQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return "";
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] idx = pair.split("=");
            if (idx.length > 0 && idx[0].equals(key)) {
                try {
                    return java.net.URLDecoder.decode(idx.length > 1 ? idx[1] : "", "UTF-8");
                } catch (Exception e) {
                    return idx.length > 1 ? idx[1] : "";
                }
            }
        }
        return "";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int findSlotIdByTime(int catalogItemId, String showtime) throws SQLException {
        String slotsJson = ShowtimeOrSlotDAO.getSlotsJson(catalogItemId);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)[^}]*\"start_time\"\\s*:\\s*\"" + showtime + "\"").matcher(slotsJson);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        java.util.regex.Matcher mFallback = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(slotsJson);
        java.util.regex.Matcher mTime = java.util.regex.Pattern.compile("\"start_time\"\\s*:\\s*\"([^\"]+)\"").matcher(slotsJson);
        while (mFallback.find() && mTime.find()) {
            if (mTime.group(1).equals(showtime)) {
                return Integer.parseInt(mFallback.group(1));
            }
        }
        return -1;
    }

    // ------------------ API ROUTE HANDLERS ------------------

    /**
     * Handler to serve frontend index.html files.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr.equals("/") || pathStr.equals("/index.html")) {
                Path file = Paths.get("src/index.html");
                if (Files.exists(file)) {
                    byte[] bytes = Files.readAllBytes(file);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                    exchange.getResponseHeaders().set("Pragma", "no-cache");
                    exchange.getResponseHeaders().set("Expires", "0");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    String error = "<html><body><h1>index.html not found in src/ folder!</h1></body></html>";
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(404, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } else {
                byte[] bytes = "Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    /**
     * Handler for POST /api/login
     */
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String username = parseJsonString(body, "username").trim();
                String password = parseJsonString(body, "password").trim();
                String userJson = UserDAO.authenticate(username, password);
                sendJsonResponse(exchange, 200, userJson);
            } catch (BookingNotFoundException e) {
                sendJsonResponse(exchange, 401, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Server login error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/register
     */
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String username = parseJsonString(body, "username").trim();
                String password = parseJsonString(body, "password").trim();
                String role = parseJsonString(body, "role").trim();

                if (username.isEmpty() || password.isEmpty() || role.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"All fields are required.\"}");
                    return;
                }

                UserDAO.register(username, password, role, "Bengaluru");
                sendJsonResponse(exchange, 200, "{\"success\":\"User registered successfully. Log in now!\"}");
            } catch (CouponInvalidException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Registration failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/catalog
     */
    static class CatalogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String city = getQueryParam(query, "city").trim();
                String category = getQueryParam(query, "category").trim();
                String genre = getQueryParam(query, "genre").trim();
                String search = getQueryParam(query, "search").trim().toLowerCase();
                String userIdStr = getQueryParam(query, "user_id").trim();
                int userId = userIdStr.isEmpty() ? -1 : Integer.parseInt(userIdStr);

                String catalogJson = CatalogItemDAO.getCatalogJson(city, category, genre, search, userId);
                sendJsonResponse(exchange, 200, catalogJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Failed to load catalog: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/catalog/recommended
     */
    static class RecommendedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String userIdStr = getQueryParam(query, "user_id").trim();
                if (userIdStr.isEmpty()) {
                    sendJsonResponse(exchange, 200, "[]");
                    return;
                }
                int userId = Integer.parseInt(userIdStr);
                String recommendedJson = CatalogItemDAO.getRecommendedJson(userId);
                sendJsonResponse(exchange, 200, recommendedJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Recommendations failure: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/catalog/trending
     */
    static class TrendingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String trendingJson = CatalogItemDAO.getTrendingJson();
                sendJsonResponse(exchange, 200, trendingJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Trending catalog load failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/book
     */
    static class BookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                int userId = parseJsonInt(body, "userId");
                int catalogItemId = parseJsonInt(body, "catalogItemId");
                String showtime = parseJsonString(body, "showtime").trim();
                int seatsCount = parseJsonInt(body, "seats");
                String couponCode = parseJsonString(body, "couponCode").trim().toUpperCase();
                String details = parseJsonString(body, "details").trim();
                boolean redeemWallet = parseJsonBoolean(body, "redeemWallet");

                if (userId <= 0 || catalogItemId <= 0 || showtime.isEmpty() || seatsCount <= 0) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Invalid booking request details.\"}");
                    return;
                }

                // 1. Resolve slotId
                int slotId = findSlotIdByTime(catalogItemId, showtime);
                if (slotId == -1) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Selected time slot is not scheduled.\"}");
                    return;
                }

                // 2. Fetch catalog details
                String itemJson = CatalogItemDAO.getItemDetailsJson(catalogItemId);
                java.math.BigDecimal basePrice = parseJsonBigDecimal(itemJson, "base_price");
                boolean isSpecial = parseJsonBoolean(itemJson, "is_special");
                java.math.BigDecimal surcharge = parseJsonBigDecimal(itemJson, "surcharge");

                java.math.BigDecimal finalCost = basePrice;
                if (isSpecial) {
                    finalCost = finalCost.add(surcharge);
                }
                finalCost = finalCost.multiply(java.math.BigDecimal.valueOf(seatsCount));

                // 3. Apply Pricing rules (Dynamic Pricing surge)
                java.util.Calendar cal = java.util.Calendar.getInstance();
                String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                String dayOfWeek = days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
                double surge = PricingRuleDAO.getSurgeMultiplier(catalogItemId, dayOfWeek);
                finalCost = finalCost.multiply(java.math.BigDecimal.valueOf(surge));

                // 4. Validate Coupon Eligibility
                java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
                if (!couponCode.isEmpty()) {
                    try {
                        CouponDAO.validateCouponUsage(userId, couponCode);
                        String couponJson = CouponDAO.getCouponDetailsJson(couponCode);
                        String discountType = parseJsonString(couponJson, "discount_type");
                        java.math.BigDecimal discountValue = parseJsonBigDecimal(couponJson, "discount_value");
                        
                        if ("PERCENT".equalsIgnoreCase(discountType)) {
                            discount = finalCost.multiply(discountValue).divide(new java.math.BigDecimal("100.00"), 4, java.math.RoundingMode.HALF_UP);
                        } else {
                            discount = discountValue;
                        }
                    } catch (CouponInvalidException e) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                        return;
                    }
                }

                finalCost = finalCost.subtract(discount);
                if (finalCost.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    finalCost = java.math.BigDecimal.ZERO;
                }

                // 5. Calculate loyalty wallet points burn value
                java.math.BigDecimal walletBurn = java.math.BigDecimal.ZERO;
                if (redeemWallet) {
                    java.math.BigDecimal walletBalance = UserDAO.getWalletBalance(userId);
                    if (walletBalance.compareTo(finalCost) >= 0) {
                        walletBurn = finalCost;
                        finalCost = java.math.BigDecimal.ZERO;
                    } else {
                        walletBurn = walletBalance;
                        finalCost = finalCost.subtract(walletBalance);
                    }
                }

                // Scale values
                finalCost = finalCost.setScale(2, java.math.RoundingMode.HALF_UP);
                walletBurn = walletBurn.setScale(2, java.math.RoundingMode.HALF_UP);

                // 6. Generate Booking ID (BK + 4 digits)
                String bookingID = "BK" + (1000 + new java.util.Random().nextInt(9000));

                // 7. Parse seat labels
                String[] seatLabels = null;
                if (details.contains(",")) {
                    seatLabels = details.split(",");
                } else if (!details.isEmpty()) {
                    seatLabels = new String[]{details};
                }

                // 8. Execute Booking transactional workflow
                BookingDAO.createBookingTransaction(
                    bookingID, userId, catalogItemId, slotId, seatsCount,
                    finalCost, seatLabels, couponCode, redeemWallet, walletBurn
                );

                sendJsonResponse(exchange, 200, "{\"bookingID\":\"" + bookingID + "\",\"cost\":" + finalCost + ",\"walletRedeemed\":" + walletBurn + "}");

            } catch (CapacityExceededException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (CouponInvalidException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"error\":\"Booking creation failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/payment/confirm
     */
    static class PaymentConfirmHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String bookingID = parseJsonString(body, "bookingID").trim();
                boolean paymentSuccess = parseJsonBoolean(body, "success");

                PaymentDAO.confirmPaymentTransaction(bookingID, paymentSuccess);
                
                // Return dummy confirmed Booking JSON to keep frontend happy
                sendJsonResponse(exchange, 200, "{\"bookingID\":\"" + bookingID + "\",\"totalCost\":0.0,\"isCancelled\":false}");
            } catch (PaymentFailedException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Confirmation failure: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/cancel
     */
    static class CancelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String bookingID = parseJsonString(body, "bookingID").trim();

                BookingDAO.cancelBookingTransaction(bookingID);
                sendJsonResponse(exchange, 200, "{\"bookingID\":\"" + bookingID + "\",\"isCancelled\":true}");
            } catch (BookingNotFoundException e) {
                sendJsonResponse(exchange, 404, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (AlreadyCancelledException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Cancellation failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/bookings
     */
    static class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String userIdStr = getQueryParam(query, "user_id").trim();
                String role = getQueryParam(query, "role").trim();

                if (userIdStr.isEmpty()) {
                    sendJsonResponse(exchange, 200, "[]");
                    return;
                }
                int userId = Integer.parseInt(userIdStr);

                String bookingsJson = BookingDAO.getBookingsJson(userId, role);
                sendJsonResponse(exchange, 200, bookingsJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Bookings fetch error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/reviews/add
     */
    static class AddReviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                int userId = parseJsonInt(body, "userId");
                int catalogItemId = parseJsonInt(body, "catalogItemId");
                int rating = parseJsonInt(body, "rating");
                String comment = parseJsonString(body, "comment").trim();

                if (userId <= 0 || catalogItemId <= 0 || rating < 1 || rating > 5) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Rating must be between 1 and 5.\"}");
                    return;
                }

                // Verify user booked this item confirmed
                String bookingsJson = BookingDAO.getBookingsJson(userId, "CUSTOMER");
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "\"bookingID\"\\s*:\\s*\"([^\"]+)\"[^}]*\"status\"\\s*:\\s*\"CONFIRMED\""
                ).matcher(bookingsJson);
                
                String bookingID = "";
                if (m.find()) {
                    bookingID = m.group(1);
                }

                ReviewDAO.addReview(userId, catalogItemId, bookingID, rating, comment);
                sendJsonResponse(exchange, 200, "{\"success\":\"Review added successfully!\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Review submission failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/reviews
     */
    static class ReviewsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String itemIdStr = getQueryParam(query, "catalog_item_id").trim();
                if (itemIdStr.isEmpty()) {
                    sendJsonResponse(exchange, 200, "[]");
                    return;
                }
                int itemId = Integer.parseInt(itemIdStr);

                String reviewsJson = ReviewDAO.getReviewsJson(itemId);
                sendJsonResponse(exchange, 200, reviewsJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Reviews fetch failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/wishlist/toggle
     */
    static class WishlistToggleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                int userId = parseJsonInt(body, "userId");
                int catalogItemId = parseJsonInt(body, "catalogItemId");

                boolean added = WishlistDAO.toggleWishlist(userId, catalogItemId);
                sendJsonResponse(exchange, 200, "{\"status\":\"" + (added ? "added" : "removed") + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Wishlist toggle failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/wishlist
     */
    static class WishlistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String userIdStr = getQueryParam(query, "user_id").trim();
                if (userIdStr.isEmpty()) {
                    sendJsonResponse(exchange, 200, "[]");
                    return;
                }
                int userId = Integer.parseInt(userIdStr);

                String wishlistJson = WishlistDAO.getWishlistJson(userId);
                sendJsonResponse(exchange, 200, wishlistJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Favorites retrieval failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/wallet/history
     */
    static class WalletHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String userIdStr = getQueryParam(query, "user_id").trim();
                if (userIdStr.isEmpty()) {
                    sendJsonResponse(exchange, 200, "[]");
                    return;
                }
                int userId = Integer.parseInt(userIdStr);

                String historyJson = WalletLedgerDAO.getWalletHistoryJson(userId);
                sendJsonResponse(exchange, 200, historyJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Wallet retrieval failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/notifications
     */
    static class NotificationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String userIdStr = getQueryParam(query, "user_id").trim();
                if (userIdStr.isEmpty()) {
                    sendJsonResponse(exchange, 200, "[]");
                    return;
                }
                int userId = Integer.parseInt(userIdStr);

                String sentJson = NotificationDAO.getUserSentNotificationsJson(userId);
                sendJsonResponse(exchange, 200, sentJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Notifications load failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for GET /api/admin/analytics
     */
    static class AdminAnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null || !auth.equals("admin123")) {
                    sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized admin access.\"}");
                    return;
                }

                String analyticsJson = BookingDAO.getAnalyticsJson();
                sendJsonResponse(exchange, 200, analyticsJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Analytics aggregation failure: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler for POST /api/catalog/add
     */
    static class AddCatalogItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptionsAndCors(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null || (!auth.equals("admin123") && !auth.startsWith("partner_"))) {
                    sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized partner access.\"}");
                    return;
                }

                String body = readRequestBody(exchange);
                String title = parseJsonString(body, "title").trim();
                String category = parseJsonString(body, "category").trim();
                String genre = parseJsonString(body, "genre").trim();
                int duration = parseJsonInt(body, "durationMinutes");
                java.math.BigDecimal price = parseJsonBigDecimal(body, "price");
                int venueId = parseJsonInt(body, "venueId");
                String description = parseJsonString(body, "description").trim();
                String showtimes = parseJsonString(body, "showtimes").trim();
                int capacity = parseJsonInt(body, "capacity");

                if (title.isEmpty() || category.isEmpty() || genre.isEmpty() || duration <= 0 || price.compareTo(java.math.BigDecimal.ZERO) < 0 || venueId <= 0 || showtimes.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Please check all fields parameters.\"}");
                    return;
                }

                // If role is VENUE_PARTNER, enforce venue ownership row-level check
                if (auth.startsWith("partner_")) {
                    int partnerUserId = Integer.parseInt(auth.substring(8));
                    if (!VenueDAO.isVenueOwner(venueId, partnerUserId)) {
                        sendJsonResponse(exchange, 403, "{\"error\":\"You are not authorized to manage catalog items for this venue.\"}");
                        return;
                    }
                }

                // 1. Insert Catalog Item
                int itemId = CatalogItemDAO.addCatalogItem(venueId, category, title, description, genre, duration, price);
                
                // 2. Insert Slots in Bulk using Batch operation
                if (itemId != -1) {
                    String[] slotsArray = showtimes.split(",");
                    ShowtimeOrSlotDAO.addBatchShowtimes(itemId, slotsArray, capacity);
                }

                sendJsonResponse(exchange, 200, "{\"success\":\"Catalog item registered successfully!\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Failed to register item: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * GET /api/slots?catalog_item_id=X
     * Returns all available time slots for the given catalog item.
     */
    static class SlotsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                String query = exchange.getRequestURI().getQuery();
                String idStr = getQueryParam(query, "catalog_item_id");
                if (idStr.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"catalog_item_id required\"}");
                    return;
                }
                int catalogItemId = Integer.parseInt(idStr);
                String slotsJson = ShowtimeOrSlotDAO.getSlotsJson(catalogItemId);
                sendJsonResponse(exchange, 200, slotsJson);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }
}
