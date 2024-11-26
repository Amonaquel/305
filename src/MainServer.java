import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    private static final int PORT = 8000;
    
    public static void main(String[] args) {
        int i = 0;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT+"...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                if (clientSocket.isConnected()) System.out.println("Client"+ i++ +"is connected");
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}