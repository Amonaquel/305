import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;

public class CarRentalServer {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/CarRental", "root", "Am1423@@")
        ) {
            out.writeUTF("Enter 1 to Login, 2 to Signup:");
            int option = in.readInt();

            if (option == 1) {
                handleLogin(in, out, conn);
            } else if (option == 2) {
                handleSignup(in, out, conn);
            } else {
                out.writeUTF("Invalid option selected.");
            }

        } catch (IOException | SQLException e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleLogin(DataInputStream in, DataOutputStream out, Connection conn) throws IOException, SQLException {
        out.writeUTF("Enter username:");
        String username = in.readUTF();
        out.writeUTF("Enter password:");
        String password = in.readUTF();

        String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                out.writeUTF("Login successful!");
                showCarListAndHandleSelection(in, out, conn, username);
            } else {
                out.writeUTF("Invalid credentials. Try again.");
            }
        }
    }

    private void handleSignup(DataInputStream in, DataOutputStream out, Connection conn) throws IOException, SQLException {
        out.writeUTF("Enter new username:");
        String username = in.readUTF();
        out.writeUTF("Enter new password:");
        String password = in.readUTF();

        String query = "INSERT INTO Users (username, password, rented_license_plate) VALUES (?, ?, NULL)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                out.writeUTF("Signup successful!");
                showCarListAndHandleSelection(in, out, conn, username);
            } else {
                out.writeUTF("Signup failed. Try again.");
            }
        }
    }

    private void showCarListAndHandleSelection(DataInputStream in, DataOutputStream out, Connection conn, String username) throws IOException, SQLException {
        ArrayList<Integer> carIds = new ArrayList<>();
        ArrayList<String> licensePlates = new ArrayList<>();

        // Fetch available cars
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

        // Update the car and user records
        try {
            conn.setAutoCommit(false);

            // Mark the car as unavailable
            String updateCarQuery = "UPDATE Cars SET available = FALSE WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateCarQuery)) {
                stmt.setInt(1, carId);
                stmt.executeUpdate();
            }

            // Update the user's rented car
            String updateUserQuery = "UPDATE Users SET rented_license_plate = ? WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateUserQuery)) {
                stmt.setString(1, licensePlate);
                stmt.setString(2, username);
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
