import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {
    private static final String COORDINATOR_IP = "127.0.0.1";
    private static final int COORDINATOR_PORT = 8081;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Distributed Library Client Application started.");

        while (true) {
            System.out.print("Enter command (search, lease, return, stats, list, quit): ");
            String userInput = scanner.nextLine().trim();
            if (userInput.isEmpty()) continue;

            if (userInput.equalsIgnoreCase("quit")) {
                // Ask coordinator to acknowledge then exit locally
                sendRequest(userInput);
                System.out.println("Exiting client application. Goodbye!");
                break;
            }

            sendRequest(userInput);
        }

        scanner.close();
    }

    private static void sendRequest(String command) {
        try (Socket socket = new Socket(COORDINATOR_IP, COORDINATOR_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(command);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_RESPONSE")) break;
                System.out.println(line);
            }

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }
}
