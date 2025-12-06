// CoordinatorServer.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CoordinatorServer {

    private static final int CLIENT_PORT = 5000;
    private List<ServerInfo> libraryServers = new ArrayList<>();

    public CoordinatorServer() {
        libraryServers.add(new ServerInfo("127.0.0.1", 6001));
        libraryServers.add(new ServerInfo("127.0.0.1", 6002));
    }

    static class ServerInfo {
        String ip;
        int port;
        ServerInfo(String ip, int port) { this.ip = ip; this.port = port; }
        public String toString() { return ip + ":" + port; }
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(CLIENT_PORT);
        System.out.println("Coordinateur en écoute sur le port " + CLIENT_PORT);

        ExecutorService clientPool = Executors.newFixedThreadPool(10);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            clientPool.submit(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("search ")) {
                    List<String> results = broadcastSearch(line.substring(7));
                    out.println("Results: " + results);

                } else if (line.startsWith("lease ")) {
                    String bookId = line.substring(6);
                    String response = forwardRequest(bookId, "lease");
                    out.println(response);

                } else if (line.startsWith("return ")) {
                    String bookId = line.substring(7);
                    String response = forwardRequest(bookId, "return");
                    out.println(response);

                } else if (line.equals("stats")) {
                    Map<String, Integer> stats = collectStats();
                    out.println("Stats: " + stats);

                } else if (line.equals("quit")) {
                    out.println("Goodbye!");
                    break;

                } else {
                    out.println("Unknown command");
                }
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private List<String> broadcastSearch(String keyword) {
        List<String> mergedResults = new ArrayList<>();
        for (ServerInfo server : libraryServers) {
            try (Socket socket = new Socket(server.ip, server.port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("search " + keyword);
                String response = in.readLine();
                if (response != null) mergedResults.add(response);

            } catch (IOException e) {
                System.out.println("Server " + server + " unreachable.");
            }
        }
        return mergedResults;
    }

    private String forwardRequest(String bookId, String command) {
        for (ServerInfo server : libraryServers) {
            try (Socket socket = new Socket(server.ip, server.port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println(command + " " + bookId);
                return in.readLine();

            } catch (IOException e) {
                // try next server
            }
        }
        return "Failed: no server available for book " + bookId;
    }

    private Map<String, Integer> collectStats() {
        Map<String, Integer> totalStats = new HashMap<>();
        for (ServerInfo server : libraryServers) {
            try (Socket socket = new Socket(server.ip, server.port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("stats");
                String response = in.readLine();
                if (response != null) {
                    for (String s : response.split(",")) {
                        String[] parts = s.split(":");
                        totalStats.put(parts[0], totalStats.getOrDefault(parts[0], 0) + Integer.parseInt(parts[1]));
                    }
                }

            } catch (IOException e) {}
        }
        return totalStats;
    }

    public static void main(String[] args) throws IOException {
        new CoordinatorServer().start();
    }
}
