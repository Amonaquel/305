import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.InputMismatchException;
public class ClientHandler implements Runnable {
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
                       new Login(in, out, conn);
                        check = true;  // Exit loop on successful login
                    } else if (option == 2) {
                        // Handle Signup
                       new SignUp(in, out, conn);
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

    






}

