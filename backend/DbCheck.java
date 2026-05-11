import java.sql.*;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://192.168.120.233:3306/ai_test_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false";
        try (Connection conn = DriverManager.getConnection(url, "root", "thinvent@123")) {
            System.out.println("=== project table IDs ===");
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT id, name FROM project")) {
                while (rs.next()) System.out.println("  id=" + rs.getLong("id") + " name=" + rs.getString("name"));
            }
            System.out.println("\n=== requirement table project_ids ===");
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT id, project_id, name FROM requirement")) {
                while (rs.next()) System.out.println("  id=" + rs.getLong("id") + " project_id=" + rs.getLong("project_id") + " name=" + rs.getString("name"));
            }
            System.out.println("\n=== assertion_result column type ===");
            try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA='ai_test_platform' AND TABLE_NAME='execution_result' AND COLUMN_NAME='assertion_result'")) {
                while (rs.next()) System.out.println("  type=" + rs.getString("DATA_TYPE") + " max_length=" + rs.getLong("CHARACTER_MAXIMUM_LENGTH"));
            }
        }
    }
}
