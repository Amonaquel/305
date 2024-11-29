import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CarRentalServer {
    private static final int PORT = 8000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT + "...");
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
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Car-Rental", "root", "root")
        ) {
            out.writeUTF("Enter 1 to Login, 2 to Signup:");
            boolean check = false;

            while (!check) {
                try {
                    int option = in.readInt();
                    if (option == 1) {
                        handleLogin(in, out, conn);
                        check = true;
                    } else if (option == 2) {
                        handleSignup(in, out, conn);
                        check = true;
                    } else {
                        out.writeUTF("Invalid option selected. Please enter 1 for Login or 2 for Signup.");
                    }
                } catch (IOException | NumberFormatException e) {
                    out.writeUTF("Invalid input. Please enter a valid number (1 or 2).");
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
                    showCarListAndHandleSelection(in, out, conn, rs.getInt("id"));
                    isAuthenticated = true;
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

        String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                out.writeUTF("Signup successful!");
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        showCarListAndHandleSelection(in, out, conn, userId);
                    }
                }
            } else {
                out.writeUTF("Signup failed. Try again.");
            }
        }
    }

    private void showCarListAndHandleSelection(DataInputStream in, DataOutputStream out, Connection conn, int userId) throws IOException, SQLException {
        ArrayList<Integer> carIds = new ArrayList<>();
        ArrayList<String> licensePlates = new ArrayList<>();

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

        out.writeUTF("Enter the numbers of the cars you want to rent (e.g., 1,2,3):");
        String input = in.readUTF();
        String[] selectedNumbers = input.split(",");
        ArrayList<Integer> selectedCarIds = new ArrayList<>();
        ArrayList<String> selectedLicensePlates = new ArrayList<>();

        for (String number : selectedNumbers) {
            try {
                int choice = Integer.parseInt(number.trim());
                if (choice < 1 || choice > carIds.size()) {
                    out.writeUTF("Invalid choice! Please select valid car numbers.");
                    return;
                }
                selectedCarIds.add(carIds.get(choice - 1));
                selectedLicensePlates.add(licensePlates.get(choice - 1));
            } catch (NumberFormatException e) {
                out.writeUTF("Invalid input format. Please enter numbers separated by commas.");
                return;
            }
        }

        try {
            conn.setAutoCommit(false);

            for (int i = 0; i < selectedCarIds.size(); i++) {
                int carId = selectedCarIds.get(i);
                String licensePlate = selectedLicensePlates.get(i);

                // Prompt for start and end date for each car
                String startDate = getValidDate(in, out, "Enter start date for car (" + licensePlate + ") (YYYY-MM-DD): ");
                String endDate = getValidDate(in, out, "Enter end date for car (" + licensePlate + ") (YYYY-MM-DD): ", startDate);

                String updateCarQuery = "UPDATE Cars SET available = FALSE WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCarQuery)) {
                    stmt.setInt(1, carId);
                    stmt.executeUpdate();
                }

                String insertRentalQuery = "INSERT INTO rentals (user_id, license_plate, start_date, end_date) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertRentalQuery)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, licensePlate);
                    stmt.setString(3, startDate);
                    stmt.setString(4, endDate);
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            out.writeUTF("Cars rented successfully! Connection will now close.");
        } catch (SQLException e) {
            conn.rollback();
            out.writeUTF("Failed to rent cars. Please try again.");
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private String getValidDate(DataInputStream in, DataOutputStream out, String prompt) throws IOException {
        String date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            try {
                Date parsedDate = sdf.parse(date);
                Date currentDate = new Date();
                if (parsedDate.before(currentDate)) {
                    out.writeUTF("Date cannot be in the past. Please enter a valid date.");
                    continue;
                }
                return date;
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }
    }

    private String getValidDate(DataInputStream in, DataOutputStream out, String prompt, String startDate) throws IOException {
        String date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        Date startParsedDate;

        try {
            startParsedDate = sdf.parse(startDate);
        } catch (ParseException e) {
            out.writeUTF("Invalid start date format.");
            return null;
        }

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            try {
                Date parsedDate = sdf.parse(date);
                if (parsedDate.before(startParsedDate)) {
                    out.writeUTF("End date cannot be before start date. Please enter a valid end date.");
                    continue;
                }
                return date;
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }
    }
}
