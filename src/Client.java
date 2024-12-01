import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 8000);

             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {


            while (true) {
                try {
                    String serverMessage = in.readUTF();
                    System.out.println(serverMessage);

                    if (serverMessage.contains("Thanks")) {
                        System.out.println("Closing connection...");
                        break;
                    }

                    if (serverMessage.contains("Enter")) { // Handles server prompts requiring user input
                       getUserinput(serverMessage,console,out);
                    }
                } catch (EOFException e) {
                    System.out.println("Server closed the connection.");
                    break;
                } catch (IOException e) {
                    System.err.println("Client error: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }catch (Exception e) {
            System.err.println("Something went wrong, Please try again. ");
        }
    }
    // Ensuring the input is neither null nor empty
    public static void getUserinput(String serverMessage, BufferedReader console, DataOutputStream out) throws IOException {
        String userInput = "";

        while (true) {  // Infinite loop until valid input
            try {
                userInput = console.readLine();
               // System.out.println("Client inputttt ======" + userInput);// uncomment this for debug
                if (userInput == null || userInput.trim().isEmpty()) {
                    throw new IllegalArgumentException("Input cannot be null or empty.\n"+ serverMessage);
                }

                if (serverMessage.contains("number")) {
                    out.writeInt(Integer.parseInt(userInput));  // Attempt to write an integer
                   // System.out.println("The value is a num======" + userInput); // uncomment this for debug
                } else {
                    out.writeUTF(userInput);  // Attempt to write a string
                }
                break;  // Exit loop if input is valid
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

}

