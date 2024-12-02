import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
public class CarList {

    private static final Lock lock = new ReentrantLock();

    public void showCarListAndHandleSelection(DataInputStream in, DataOutputStream out, Connection conn, int userId) throws IOException, SQLException {
       
        
        ArrayList<Integer> carIds = new ArrayList<>();
        ArrayList<String> licensePs = new ArrayList<>();
        
            out.writeUTF("Displaying available cars ...\n");
            String carQuery = "SELECT id, name, license_plate FROM cars WHERE available = TRUE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(carQuery)) {
                int index = 1;
                while (rs.next()) {
                    out.writeUTF(index + ". " + rs.getString("name") + " (License Plate: " + rs.getString("license_plate") + ")");
                    carIds.add(rs.getInt("id"));
                    licensePs.add(rs.getString("license_plate"));
                    index++;
                }
            }

            if (carIds.isEmpty()) {
                out.writeUTF("No cars are available for rent.");
                Menu menu = new Menu();
                menu.execute(in, out, conn, userId);
            }

            int carId = 0;
            String licenseP = null;
            boolean correct = false;

            while (!correct) {

                out.writeUTF( "Enter the car num you want to rent :");
                String input = in.readUTF();

                try {

                    int choice = Integer.parseInt(input.trim());
                    if (choice < 1 || choice > carIds.size()) {
                        out.writeUTF("Invalid choice! Please select valid car numbers.");
                    } else {
                        carId = (carIds.get(choice - 1));
                        licenseP = (licensePs.get(choice - 1));
                        correct = true;
                    }
                } catch (NumberFormatException e) {
                    out.writeUTF("Invalid input format. (only one number ex : 1  )");
                }
            }
            try {
                conn.setAutoCommit(false);


                // Prompt for start and end date for each car
                CheckDate check = new CheckDate();
                String startDate = check.getValidDate(in, out, "Enter start date for car (" + licenseP + ") (YYYY-MM-DD): ");
                String endDate = check.getValidDate(in, out, "Enter end date for car (" + licenseP + ") (YYYY-MM-DD): ", startDate);
                lock.lock();
                try {
                String availabilityCheckQuery = "SELECT available FROM cars WHERE id = ? FOR UPDATE";
                try (PreparedStatement checkStmt = conn.prepareStatement(availabilityCheckQuery)) {
                    checkStmt.setInt(1, carId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next() || !rs.getBoolean("available")) {
                            out.writeUTF("Sorry, the selected car is no longer available.");
                           showCarListAndHandleSelection(in, out, conn, userId);
                        }
                    }
                }

                // Update car availability
                String updateCarQuery = "UPDATE cars SET available = FALSE WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCarQuery)) {
                    stmt.setInt(1, carId);
                    stmt.executeUpdate();
                }

                // Insert rental information
                String insertRentalQuery = "INSERT INTO rentals (user_id, license_plate, start_date, end_date) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertRentalQuery)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, licenseP);
                    stmt.setString(3, startDate);
                    stmt.setString(4, endDate);
                    stmt.executeUpdate();
                }

                conn.commit();
                out.writeUTF("Car rented successfully!\n");
           } finally {
                lock.unlock();  // Always release the lock
           }
                
                //out.writeUTF("Enter number : 1- to rent another car , 2- to Exit");
                //userInput = in.readInt();
                Menu menu = new Menu();
                menu.execute(in, out, conn, userId);
            } catch (SQLException e) {
                conn.rollback();
                out.writeUTF("Failed to rent cars. Please try again.");
                throw e;
            }

       // out.writeUTF("Tanks for using our car rent app :) ");
        conn.setAutoCommit(true);

    }
}
