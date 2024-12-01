import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;

public class SignUp {

    public void execute(DataInputStream in, DataOutputStream out, Connection conn) throws IOException, SQLException {
        out.writeUTF( "Enter new username:");
        String username = in.readUTF();
        out.writeUTF("Enter new password:");
        String password = in.readUTF();

        String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                out.writeUTF("Signup successful!");
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        Menu menu = new Menu();
                        menu.execute(in, out, conn, userId);
                    }
                }
            } else {
                out.writeUTF("Signup failed. Try again.");
            }
        }
    }
}
