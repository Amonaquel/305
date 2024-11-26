

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SignUp {

     public SignUp(DataInputStream in, DataOutputStream out, Connection conn) throws IOException, SQLException {
        out.writeUTF("Enter new username:");
        String username = in.readUTF();
        out.writeUTF("Enter new password:");
        String password = in.readUTF();

        String query = "INSERT INTO Users (username, password, rented_license_plate, start_date, end_date) VALUES (?, ?, NULL, NULL, NULL)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                CarList c = new CarList();
                c.getCarList(in, out, conn, username);
            } else {
                out.writeUTF("Signup failed. Try again.");
            }
        }
    }

}
