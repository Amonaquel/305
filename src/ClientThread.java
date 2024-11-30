import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.*;

class ClientThread implements Runnable {
    private final Socket clientSocket;

    public ClientThread(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/car_renatl", "root", "root")
        ) {

            boolean check = false;

            while (!check) {
                out.writeUTF("Enter number: 1- to Login, 2- to Signup : ");

                try {
                    int option = in.readInt();
                    if (option == 1) {
                        Login loginhandler = new Login();
                        loginhandler.execute(in, out, conn);
                        check = true;
                    } else if (option == 2) {
                        SignUp signuphandler = new SignUp();
                        signuphandler.execute(in, out, conn);
                        check = true;
                    }else {
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


    public static String checknull(DataInputStream in, DataOutputStream out, String message) {
        String userInput = "";
        boolean flag = false;
        while (!flag) {
            try {
                out.writeUTF(message);
                userInput = in.readUTF();
               // out.writeUTF("Threadeduserinput ========" + userInput); // uncomment this for debug
                if (userInput != null && !userInput.trim().isEmpty()) {
                    flag = true;
                } else {
                    throw new IllegalArgumentException("Input cannot be null or empty.");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return userInput;
    }




}