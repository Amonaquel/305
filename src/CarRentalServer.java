import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class CarRentalServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8000)) {
            System.out.println("Server is running...");

            // Update the connection string with your database credentials.
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/carrental", "root", "Asdf/2003");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected.");
                new ClientHandler(socket, conn).start();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private final Connection conn;

    public ClientHandler(Socket socket, Connection conn) {
        this.socket = socket;
        this.conn = conn;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                int option = in.readInt();

                switch (option) {
                    case 1 -> handleLogin(in, out);
                    case 2 -> handleSignup(in, out);
                    case 3 -> handleGetCars(out);
                    case 4 -> handleRentCar(in, out);
                    case 5 -> handleMyRentals(in, out);
                    default -> {
                        System.out.println("Invalid option received.");
                        return;
                    }
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleLogin(ObjectInputStream in, ObjectOutputStream out) throws IOException, SQLException {
        String username = in.readUTF();
        String password = in.readUTF();

        String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                out.writeUTF("SUCCESS");
            } else {
                out.writeUTF("FAILED");
            }
            out.flush();
        }
    }

    private void handleSignup(ObjectInputStream in, ObjectOutputStream out) throws IOException, SQLException {
        String username = in.readUTF();
        String password = in.readUTF();

        String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            out.writeUTF("SUCCESS");
            out.flush();
        } catch (SQLException e) {
            out.writeUTF("FAILED");
            out.flush();
        }
    }

    private void handleGetCars(ObjectOutputStream out) throws IOException, SQLException {
        ArrayList<String> cars = new ArrayList<>();
        String query = "SELECT name, license_plate FROM Cars WHERE available = TRUE";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String car = rs.getString("name") + " (License: " + rs.getString("license_plate") + ")";
                cars.add(car);
            }
        }
        out.writeObject(cars);
        out.flush();
    }

    private void handleRentCar(ObjectInputStream in, ObjectOutputStream out) throws IOException, SQLException {
        String username = in.readUTF();
        String licensePlate = in.readUTF();
        String startDate = in.readUTF();
        String endDate = in.readUTF();

        // Validate date format using SimpleDateFormat
        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            out.writeUTF("FAILED: Invalid date format. Expected format: yyyy-MM-dd");
            out.flush();
            return;
        }

        // Check if startDate is before endDate
        if (startDate.compareTo(endDate) > 0) {
            out.writeUTF("FAILED: Start date cannot be after end date.");
            out.flush();
            return;
        }

        String userQuery = "SELECT id FROM Users WHERE username = ?";
        try (PreparedStatement userStmt = conn.prepareStatement(userQuery)) {
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();
            if (userRs.next()) {
                int userId = userRs.getInt("id");

                String carQuery = "SELECT id FROM Cars WHERE license_plate = ? AND available = TRUE";
                try (PreparedStatement carStmt = conn.prepareStatement(carQuery)) {
                    carStmt.setString(1, licensePlate);
                    ResultSet carRs = carStmt.executeQuery();
                    if (carRs.next()) {
                        int carId = carRs.getInt("id");

                        String rentQuery = "INSERT INTO rentals (user_id, license_plate, start_date, end_date) VALUES (?, ?, ?, ?)";
                        String updateQuery = "UPDATE Cars SET available = FALSE WHERE license_plate = ?";
                        try (PreparedStatement rentStmt = conn.prepareStatement(rentQuery);
                             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            rentStmt.setInt(1, userId);
                            rentStmt.setString(2, licensePlate);
                            rentStmt.setString(3, startDate);
                            rentStmt.setString(4, endDate);
                            rentStmt.executeUpdate();

                            updateStmt.setString(1, licensePlate);
                            updateStmt.executeUpdate();

                            out.writeUTF("SUCCESS");
                            out.flush();
                        }
                    } else {
                        out.writeUTF("FAILED: Car not available.");
                        out.flush();
                    }
                }
            } else {
                out.writeUTF("FAILED: User not found.");
                out.flush();
            }
        }
    }

    private boolean isValidDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);  // Ensure strict parsing (no automatic adjustments)
        try {
            sdf.parse(date);  // Try parsing the date string
            return true;
        } catch (ParseException e) {
            return false;
        }
    }


    private void handleMyRentals(ObjectInputStream in, ObjectOutputStream out) throws IOException, SQLException {
        String username = in.readUTF();
        ArrayList<String> rentals = new ArrayList<>();

        String query = "SELECT Cars.name, rentals.start_date, rentals.end_date FROM rentals " +
                "JOIN Cars ON rentals.license_plate = Cars.license_plate " +
                "JOIN Users ON rentals.user_id = Users.id " +
                "WHERE Users.username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String carName = rs.getString("name");
                String startDate = rs.getString("start_date");
                String endDate = rs.getString("end_date");
                rentals.add(carName + " (" + startDate + " - " + endDate + ")");
            }
        }
        out.writeObject(rentals);
        out.flush();
    }
}
