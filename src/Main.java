import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Set a timeout for the socket connection
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", 12345), 5000); // 5 seconds timeout

            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader console = new BufferedReader(new InputStreamReader(System.in))
            ) {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                    String userInput = console.readLine();
                    out.println(userInput);
                }
            } catch (IOException e) {
                System.err.println("Error in communication: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Error in client connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
