

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;



public class Login {
    public Login(DataInputStream in , DataOutputStream out, Connection conn) throws IOException, SQLException{
        String username;
        String password;
        boolean isAuthenticated = false;
    
        // Keep asking for username and password until authentication is successful
        while (!isAuthenticated) {
            out.writeUTF("Enter username:");
            username = in.readUTF();
            out.writeUTF("Enter password:");
            password = in.readUTF();
    
            String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
    
                if (rs.next()) {
                    out.writeUTF("Login successful!");
                    CarList c = new CarList();
                  c.getCarList(in, out, conn, username);
                    isAuthenticated = true; // Exit the loop if credentials are correct
                } else {
                    out.writeUTF("Invalid username or password. Please try again.");
                }
            }
        }
    }
   
}