package com.dhruv.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.dhruv.security.EncryptionService;

public class DataBaseConnection {

    public static class PasswordEntry {
        public int id;
        public String website;
        public String username;
        public String password;
        public String category;

        public PasswordEntry(int id, String website, String username, String password, String category) {
            this.id = id;
            this.website = website;
            this.username = username;
            this.password = password;
            this.category = category;
        }
    }

    private static final String URL = "jdbc:sqlite:password_manager.db";

    public static Connection connect() throws SQLException {
        try {
            // Force load SQLite driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found.", e);
        }

        return DriverManager.getConnection(URL);
    }

    public static void deletePassword(String website, String username) {
        String sql = "DELETE FROM passwords WHERE website = ? AND username = ?";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, website);
            pstmt.setString(2, username);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Password deleted successfully!");
            } else {
                System.out.println("No matching record found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertPassword(String website, String username, String password, String category) {

        String sql = "INSERT INTO passwords(website, username, password, category) VALUES(?,?,?,?)";

        try (Connection conn = connect();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, website);
            pstmt.setString(2, username);
            String encryptedPassword = EncryptionService.encrypt(password);
            pstmt.setString(3, encryptedPassword);
            pstmt.setString(4, category == null || category.trim().isEmpty() ? "General" : category);

            pstmt.executeUpdate();
            System.out.println("Password saved successfully!");

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println("Password already exists for this website and username.");
            } else {
                e.printStackTrace();
            }
        }
    }

    public static void updatePassword(String website, String username, String newPassword, String category) {
        String sql = "UPDATE passwords SET password = ?, category = ? WHERE website = ? AND username = ?";

        try (Connection conn = connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, EncryptionService.encrypt(newPassword));
            pstmt.setString(2, category == null || category.trim().isEmpty() ? "General" : category);
            pstmt.setString(3, website);
            pstmt.setString(4, username);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Password updated successfully!");
            } else {
                System.out.println("No matching record found to update.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {

        String sqlPasswords = """
                CREATE TABLE IF NOT EXISTS passwords (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    website TEXT NOT NULL,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL,
                    category TEXT DEFAULT 'General',
                    UNIQUE(website, username)
                );
                """;

        String sqlSettings = """
                CREATE TABLE IF NOT EXISTS settings (
                    id INTEGER PRIMARY KEY,
                    master_hash TEXT NOT NULL,
                    salt TEXT NOT NULL
                );
                """;

        try (Connection conn = connect();
                java.sql.Statement stmt = conn.createStatement()) {

            stmt.execute(sqlPasswords);
            stmt.execute(sqlSettings);
            
            // Upgrade existing table to add category column if missing
            try {
                stmt.execute("ALTER TABLE passwords ADD COLUMN category TEXT DEFAULT 'General'");
            } catch (SQLException ignored) {
                // Column likely already exists
            }
            
            System.out.println("Tables initialized successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String searchPassword(String website, String username) {

        String sql = "SELECT * FROM passwords WHERE website LIKE '%' || ? || '%' COLLATE NOCASE " +
                     "AND username LIKE '%' || ? || '%' COLLATE NOCASE";
        StringBuilder result = new StringBuilder();

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, website);
            pstmt.setString(2, username);

            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                String cat = "Uncategorized";
                try { cat = rs.getString("category"); } catch (Exception ignored) {}
                
                result.append(
                        rs.getInt("id") + " | " +
                                rs.getString("website") + " | " +
                                rs.getString("username") + " | [" + cat + "] | " +
                                EncryptionService.decrypt(rs.getString("password"))).append("\n");
            }
            if (!found) {
                return "No password found.";
            }

        } catch (SQLException e) {
            return "Error searching password: " + e.getMessage();
        }

        return result.toString();
    }

    public static String viewAllPasswords() {

        String sql = "SELECT * FROM passwords";
        StringBuilder result = new StringBuilder();

        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String cat = "Uncategorized";
                try { cat = rs.getString("category"); } catch (Exception ignored) {}
                
                result.append(
                        rs.getInt("id") + " | " +
                                rs.getString("website") + " | " +
                                rs.getString("username") + " | [" + cat + "] | " +
                                EncryptionService.decrypt(rs.getString("password"))).append("\n");
            }

        } catch (SQLException e) {
            return "Error fetching passwords.";
        }

        if (result.length() == 0) {
            return "No passwords stored.";
        }

        return result.toString();
    }

    public static List<PasswordEntry> getAllPasswordEntries() {
        List<PasswordEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM passwords";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String cat = "General";
                try { cat = rs.getString("category"); } catch (Exception ignored) {}
                if (cat == null) cat = "General";
                
                list.add(new PasswordEntry(
                        rs.getInt("id"),
                        rs.getString("website"),
                        rs.getString("username"),
                        EncryptionService.decrypt(rs.getString("password")),
                        cat
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Get the master password hash and salt from the database.
     * Returns a String array [hash, salt] if a master password is set, or null if
     * not set.
     */
    public static String[] getMasterPassword() {
        String sql = "SELECT master_hash, salt FROM settings WHERE id = 1";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new String[] { rs.getString("master_hash"), rs.getString("salt") };
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // No master password set yet
    }

    /**
     * Save or update the master password hash and salt in the settings table.
     */
    public static void saveMasterPassword(String hash, String salt) {
        // First, check if a settings record already exists
        String checkSql = "SELECT COUNT(*) FROM settings WHERE id = 1";
        String insertSql = "INSERT INTO settings (id, master_hash, salt) VALUES (1, ?, ?)";
        String updateSql = "UPDATE settings SET master_hash = ?, salt = ? WHERE id = 1";

        try (Connection conn = connect()) {
            // Check if record exists
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(checkSql)) {
                rs.next();
                boolean exists = rs.getInt(1) > 0;

                // Insert or update accordingly
                String sql = exists ? updateSql : insertSql;
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, hash);
                    pstmt.setString(2, salt);
                    pstmt.executeUpdate();
                    System.out.println("Master password saved successfully!");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}