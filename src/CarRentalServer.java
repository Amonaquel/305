import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.locks.*;

public class CarRentalServer {
    private static final int PORT = 12345;
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            ) {
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/CarRental", "root", "Am1423@@");

                out.println("Enter 1 to Login, 2 to Signup:");
                String option = in.readLine();

                if ("1".equals(option)) {
                    out.println("Enter username:");
                    String username = in.readLine();
                    out.println("Enter password:");
                    String password = in.readLine();
                    if (authenticateUser(conn, username, password)) {
                        out.println("Login successful! Redirecting to the car list...");
                        handleCarSelection(out, in, conn, username);
                    } else {
                        out.println("Invalid credentials!");
                    }
                } else if ("2".equals(option)) {
                    out.println("Enter new username:");
                    String username = in.readLine();
                    out.println("Enter new password:");
                    String password = in.readLine();
                    if (signupUser(conn, username, password)) {
                        out.println("Signup successful! Redirecting to the car list...");
                        handleCarSelection(out, in, conn, username);
                    } else {
                        out.println("Signup failed. Username might already exist.");
                    }
                } else {
                    out.println("Invalid option!");
                }
                conn.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean authenticateUser(Connection conn, String username, String password) throws SQLException {
            String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        }

        private boolean signupUser(Connection conn, String username, String password) throws SQLException {
            String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                return stmt.executeUpdate() > 0;
            }
        }

        private void handleCarSelection(PrintWriter out, BufferedReader in, Connection conn, String username) throws IOException, SQLException {
            ArrayList<String> carList = new ArrayList<>();
            ArrayList<Integer> carIds = new ArrayList<>();

            // Fetch available cars
            String carQuery = "SELECT id, name, license_plate FROM Cars WHERE available = TRUE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(carQuery)) {
                int index = 1;
                while (rs.next()) {
                    carList.add(index + ". " + rs.getString("name") + " (License Plate: " + rs.getString("license_plate") + ")");
                    carIds.add(rs.getInt("id"));
                    index++;
                }
            }

            if (carList.isEmpty()) {
                out.println("No cars are available for rent.");
                return;
            }

            // Display the list of cars
            out.println("Available cars:");
            for (String car : carList) {
                out.println(car);
            }

            // Prompt user to select a car
            out.println("Enter the number of the car you want to rent:");
            int choice;
            try {
                choice = Integer.parseInt(in.readLine());
                if (choice < 1 || choice > carIds.size()) {
                    out.println("Invalid choice! Please select a valid car number.");
                    return;
                }
            } catch (NumberFormatException e) {
                out.println("Invalid input! Please enter a number.");
                return;
            }

            int carId = carIds.get(choice - 1);

            // Check if the car ID is valid and available
            if (!isCarAvailable(conn, carId)) {
                out.println("Invalid car ID or car is not available.");
                return;
            }

            out.println("Enter reservation date (YYYY-MM-DD):");
            String date = in.readLine();

            // Reserve the car
            try {
                lock.lock();
                String insertQuery = "INSERT INTO Reservations (user_id, car_id, reservation_date) VALUES ((SELECT id FROM Users WHERE username = ?), ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setString(1, username);
                    stmt.setInt(2, carId);
                    stmt.setDate(3, java.sql.Date.valueOf(date));
                    stmt.executeUpdate();
                }

                String updateCarQuery = "UPDATE Cars SET available = FALSE WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCarQuery)) {
                    stmt.setInt(1, carId);
                    stmt.executeUpdate();
                }

                out.println("Car booked successfully!");
            } catch (SQLException e) {
                out.println("Failed to book car. Please try again.");
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        private boolean isCarAvailable(Connection conn, int carId) throws SQLException {
            String query = "SELECT available FROM Cars WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, carId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getBoolean("available");
                } else {
                    return false; // Car does not exist
                }
            }
        }
    }
}
