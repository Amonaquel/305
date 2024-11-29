import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 8000);

             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {


            while (true) {
                try {
                    String serverMessage = in.readUTF();
                    System.out.println(serverMessage);

                    if (serverMessage.contains("Connection will now close")) {
                        System.out.println("Closing connection...");
                        break;
                    }

                    if (serverMessage.contains("Enter")) { // Handles server prompts requiring user input
                       getUserinput(serverMessage,console,out);
                    }
                } catch (EOFException e) {
                    System.out.println("hellooooooooooo");
                    System.out.println(e.getMessage());
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
        }
    }

    public static void getUserinput(String serverMessage, BufferedReader console, DataOutputStream out) throws IOException {
        String userInput = "";
        while  (userInput.isEmpty())  {
            try {
                userInput = console.readLine();
                if (userInput == null || userInput.trim().isEmpty())  {
                    throw new IllegalArgumentException("Input cannot be null or empty.");
                }
            } catch (Exception e) {
                System.out.println("here is the error ");
                System.out.println(e.getMessage());
            }
        }


        try {
            if (serverMessage.contains("number of the car") || serverMessage.contains("1 to Login, 2 to Signup")) {
                out.writeInt(Integer.parseInt(userInput));
            } else {
                out.writeUTF(userInput);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            getUserinput(serverMessage,console,out);
        }
    }
}

