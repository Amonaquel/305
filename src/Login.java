import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login {

    public void execute(DataInputStream in, DataOutputStream out, Connection conn) throws IOException, SQLException {
        String username;
        String password;
        boolean isAuthenticated = false;

        while (!isAuthenticated) {
            out.writeUTF( "Enter username:");
            username = in.readUTF();
             out.writeUTF("Enter password:");
            password = in.readUTF();

            String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    out.writeUTF("Login successful!\n"+"Hello "+ rs.getString("username"));
                    Menu menu = new Menu();
                    menu.execute(in, out, conn, rs.getInt("id"));
                    isAuthenticated = true;
                } else {
                    out.writeUTF("Invalid username or password. Please try again.");
                }
            }
        }
    }
}
