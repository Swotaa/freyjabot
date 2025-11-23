package basecode;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.*; // Not a very good practice tho...
import java.time.OffsetDateTime;

public class DatabaseManager {
    private final String DB_URL;
    private Connection connection;

    public DatabaseManager(String url) {
        this.DB_URL = url;
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("✅ Database connected successfully!");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
        }
    }

    private void initializeDatabase() {
        String createEventsTable = """
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                date TEXT NOT NULL,
                location TEXT,
                duration INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                user_id TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createGeoguessrTable = """
                CREATE TABLE IF NOT EXISTS geoguessr (
                id TEXT PRIMARY KEY,
                team1Name TEXT NOT NULL,
                team2Name TEXT NOT NULL,
                team1Points INTEGER DEFAULT 0,
                team2Points INTEGER DEFAULT 0
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createEventsTable);
            stmt.execute(createUsersTable);
            stmt.execute(createGeoguessrTable);
            System.out.println("✅ Tables created/verified!");
        } catch (SQLException e) {
            System.err.println("❌ Table creation error: " + e.getMessage());
        }
    }

    public void saveEvent(String eventId, String name, String description, String date, String location, int duration) {
        String sql = "INSERT INTO events (event_id, guild_id, name, description, date, location, duration) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eventId);
            pstmt.setString(2, name);
            pstmt.setString(3, description);
            pstmt.setString(4, date);
            pstmt.setString(5, location);
            pstmt.setInt(6, duration);
            pstmt.executeUpdate();
            System.out.println("✅ Event saved to database!");
        } catch (SQLException e) {
            System.err.println("❌ Save error: " + e.getMessage());
        }
    }

    public ResultSet getEventbyId(String eventId) {
        String sql = "SELECT * FROM events WHERE event_id = ? ORDER BY date DESC";

        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, eventId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.err.println("❌ Query error: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteEvent(String eventId) {
        String sql = "DELETE FROM events WHERE event_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eventId);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Delete error: " + e.getMessage());
            return false;
        }
    }

    public ResultSet getAllUpcomingEvents() {
        String sql = "SELECT * FROM events ORDER BY date ASC";

        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.err.println("❌ Query error: " + e.getMessage());
            return null;
        }
    }

    public void saveUser(String userId, String username) {
        String sql = "INSERT INTO users (user_id, username) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            System.out.println("✅ Registered to database!");
        } catch (SQLException e) {
            System.err.println("❌ Register error: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Close error: " + e.getMessage());
        }
    }
}
