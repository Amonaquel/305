import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            while (true) {
                String serverMessage = in.readUTF(); // Receive a message from the server
                System.out.println(serverMessage); // Print the server's message

                // Break if the process is completed
                if (serverMessage.contains("Car rented successfully!") || serverMessage.contains("No cars are available")) {
                    break;
                }

                // Send input to the server if needed
                if (serverMessage.contains("Enter")) { // Prompts requiring user input
                    String userInput = console.readLine();
                    try {
                        // Send appropriate data types (int for options, strings otherwise)
                        if (serverMessage.contains("number of the car") || serverMessage.contains("1 to Login, 2 to Signup")) {
                            out.writeInt(Integer.parseInt(userInput));
                        } else {
                            out.writeUTF(userInput);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a number.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
