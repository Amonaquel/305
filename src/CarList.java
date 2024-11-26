
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class CarList {
        public void getCarList(DataInputStream in, DataOutputStream out, Connection conn, String username) throws IOException, SQLException {
        ArrayList<Integer> carIds = new ArrayList<>();
        ArrayList<String> licensePlates = new ArrayList<>();

        // Fetch available cars only
        String carQuery = "SELECT id, name, license_plate FROM Cars WHERE available = TRUE";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(carQuery)) {
            int index = 1;
            while (rs.next()) {
                out.writeUTF(index + ". " + rs.getString("name") + " (License Plate: " + rs.getString("license_plate") + ")");
                carIds.add(rs.getInt("id"));
                licensePlates.add(rs.getString("license_plate"));
                index++;
            }
        }

        if (carIds.isEmpty()) {
            out.writeUTF("No cars are available for rent.");
            return;
        }

        out.writeUTF("Enter the number of the car you want to rent:");
        int choice;
        try {
            choice = in.readInt();
            if (choice < 1 || choice > carIds.size()) {
                out.writeUTF("Invalid choice! Please select a valid car number.");
                return;
            }
        } catch (IOException e) {
            out.writeUTF("Invalid input. Please enter a number.");
            return;
        }

        int carId = carIds.get(choice - 1);
        String licensePlate = licensePlates.get(choice - 1);

        // Get and validate rental dates
        ValidateData v = new ValidateData();
        String startDate = v.getValidDate(in, out, "Enter start date (YYYY-MM-DD): ");
        String endDate = v.getValidDate(in, out, "Enter end date (YYYY-MM-DD): ", startDate);

        // Update the car and user records
        try {
            conn.setAutoCommit(false);

            // Mark the car as unavailable
            String updateCarQuery = "UPDATE Cars SET available = FALSE WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateCarQuery)) {
                stmt.setInt(1, carId);
                stmt.executeUpdate();
            }

            // Update the user's rented car and rental dates
            String updateUserQuery = "UPDATE Users SET rented_license_plate = ?, start_date = ?, end_date = ? WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateUserQuery)) {
                stmt.setString(1, licensePlate);
                stmt.setString(2, startDate);
                stmt.setString(3, endDate);
                stmt.setString(4, username);
                stmt.executeUpdate();
            }

            conn.commit();
            out.writeUTF("Car rented successfully!");

        } catch (SQLException e) {
            conn.rollback();
            out.writeUTF("Failed to rent car. Please try again.");
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
