package da_ltm_test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private final String url = "jdbc:mysql://localhost:3306/da_ltm?useSSL=false&serverTimezone=UTC";
    private final String user = "root";  // replace with your DB username
    private final String password = "huynhthiphuoc123@";  // replace with your DB password

    public Connection connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load the driver
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error connecting to the database: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("MySQL JDBC Driver not found.");
        }
    }
}
