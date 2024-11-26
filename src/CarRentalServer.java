import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.InputMismatchException;

public class CarRentalServer {
    private static final int PORT = 8000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT+"...");
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
            boolean check = false;  // To track whether a valid option is chosen
            while (!check) {
                try {
                    // Read the option (1 for Login, 2 for Signup)
                    int option = in.readInt();

                    if (option == 1) {
                        // Handle Login
                        handleLogin(in, out, conn);
                        check = true;  // Exit loop on successful login
                    } else if (option == 2) {
                        // Handle Signup
                        handleSignup(in, out, conn);
                        check = true;  // Exit loop on successful signup
                    } else {
                        out.writeUTF("Invalid option selected. Please enter 1 for Login or 2 for Signup.");
                        // Consume the invalid input from the buffer
                        in.readLine();  // Clears the invalid input in case of wrong format or non-integer input
                    }
                } catch (IOException | InputMismatchException e) {
                    // If an exception occurs, print the message and ask for the input again
                    out.writeUTF("Invalid input. Please enter a valid number (1 or 2).");
                    in.readLine();  // Clear invalid input from the buffer
                }
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
                    showCarListAndHandleSelection(in, out, conn, username);
                    isAuthenticated = true; // Exit the loop if credentials are correct
                } else {
                    out.writeUTF("Invalid username or password. Please try again.");
                }
            }
        }
    }


    private void handleSignup(DataInputStream in, DataOutputStream out, Connection conn) throws IOException, SQLException {
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
        String startDate = getValidDate(in, out, "Enter start date (YYYY-MM-DD): ");
        String endDate = getValidDate(in, out, "Enter end date (YYYY-MM-DD): ", startDate);

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

    private String getValidDate(DataInputStream in, DataOutputStream out, String prompt) throws IOException {
        String date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            // Validate the date
            try {
                Date parsedDate = sdf.parse(date);
                Date currentDate = new Date();
                if (parsedDate.before(currentDate)) {
                    out.writeUTF("Date cannot be in the past. Please enter a valid date.");
                    continue;
                }
                break; // Valid date entered
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }

        return date;
    }

    private String getValidDate(DataInputStream in, DataOutputStream out, String prompt, String startDate) throws IOException {
        String date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        Date startParsedDate = null;

        try {
            startParsedDate = sdf.parse(startDate);
        } catch (ParseException e) {
            out.writeUTF("Invalid start date format.");
            return null;
        }

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            // Validate the date
            try {
                Date parsedDate = sdf.parse(date);
                if (parsedDate.before(startParsedDate)) {
                    out.writeUTF("End date cannot be before start date. Please enter a valid end date.");
                    continue;
                }
                break; // Valid end date entered
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }

        return date;
    }
}
