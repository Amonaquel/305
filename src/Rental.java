import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;

public class Rental {
    public void execute(DataInputStream in, DataOutputStream out, Connection conn, int userId) throws IOException {
       
        // joining the car with rental to retrive the car name that crossponding the rental one 
        String carQuery = "SELECT cars.name AS car_name, cars.license_plate, rentals.start_date, rentals.end_date FROM rentals INNER JOIN cars ON rentals.license_plate = cars.license_plate WHERE rentals.user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(carQuery)) {
            stmt.setInt(1, userId); 
            ResultSet rs = stmt.executeQuery(); 
            int index = 1;

            if (!rs.next()) {
                out.writeUTF("You did not rent any car");
            } else {
                out.writeUTF("Displaying your rental cars ...\n");
                do {
                    String carName = rs.getString("car_name");
                    String licensePlate = rs.getString("license_plate");
                    String startDate = rs.getString("start_date");
                    String endDate = rs.getString("end_date");

                    out.writeUTF(index + ". Car Name: " + carName + ", License Plate: " + licensePlate +
                            ", Start Date: " + startDate + ", End Date: " + endDate);
                    index++;
                } while (rs.next());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Menu menu = new Menu();
        menu.execute(in, out, conn, userId);

    }
}
