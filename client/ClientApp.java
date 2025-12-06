import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

    private static final String COORDINATOR_IP = "127.0.0.1";
    private static final int COORDINATOR_PORT = 8080;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Distributed Library Client Application started.");

        while (true) {
            System.out.print("Enter command (search, lease, stats, etc.) or 'quit': ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("quit")) {
                System.out.println("Exiting client application. Goodbye!");
                break;
            }

            sendRequestToCoordinator(userInput);
        }
        scanner.close();
    }

    private static void sendRequestToCoordinator(String command) {
        try (
            Socket clientSocket = new Socket(COORDINATOR_IP, COORDINATOR_PORT);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            System.out.println("[Client] Sending command: " + command);
            out.println(command);

            System.out.println("\n[Coordinator Response]:");
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                System.err.println("Error: Could not connect to Coordinator. Ensure the Coordinator is running on " + COORDINATOR_IP + ":" + COORDINATOR_PORT);
            } else {
                System.err.println("An I/O error occurred: " + e.getMessage());
            }
        }
    }
}
