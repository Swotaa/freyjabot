package basecode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// This class is used to manage the database (create, insert, delete, update, etc...)
public class DatabaseManager {
    private Connection connection;

    // Constructor
    public DatabaseManager(String url) {
        try {
            connection = DriverManager.getConnection(url);
            System.out.println("✅ Database connected successfully!");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
        }
    }

    // This creates the tables and base if not already created
    private void initializeDatabase() {
        String createEventsTable = """
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id TEXT NOT NULL,
                guild_id TEXT NOT NULL,
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

    // This insert an event created through the command /event
    public void saveEvent(String eventId, String guildId,String name, String description, String date, String location, int duration) {
        String sql = "INSERT INTO events (event_id, guild_id, name, description, date, location, duration) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eventId);
            pstmt.setString(2, guildId);
            pstmt.setString(3, name);
            pstmt.setString(4, description);
            pstmt.setString(5, date);
            pstmt.setString(6, location);
            pstmt.setInt(7, duration);
            pstmt.executeUpdate();
            System.out.println("✅ Event saved to database!");
        } catch (SQLException e) {
            System.err.println("❌ Save error: " + e.getMessage());
        }
    }

    // We can get an event if we need to ...
    public ResultSet getEventById(String eventId) {
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

    // This deletes an event created through the command /event
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

    // This is used by the reminder system
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

    // This is used by the register command to register a user
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

    // Close the base properly, automatically called by BotMain in the hook
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
