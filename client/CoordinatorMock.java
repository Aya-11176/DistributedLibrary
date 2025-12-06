import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class CoordinatorMock {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080);
        System.out.println("Mock Coordinator listening on port 8080...");
        while (true) {
            Socket client = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            String command = in.readLine();
            System.out.println("Received: " + command);
            out.println("Coordinator received: " + command);
            out.println("Result 1");
            out.println("Result 2");

            client.close();
        }
    }
}