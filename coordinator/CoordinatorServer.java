import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CoordinatorServer {
    private static final int CLIENT_PORT = 8081;
    private final List<ServerInfo> libraryServers = new ArrayList<>();

    public CoordinatorServer() {
        libraryServers.add(new ServerInfo("127.0.0.1", 9001));
        libraryServers.add(new ServerInfo("127.0.0.1", 9002));
        libraryServers.add(new ServerInfo("127.0.0.1", 9003));
    }

    static class ServerInfo {
        final String ip;
        final int port;
        ServerInfo(String ip, int port) { this.ip = ip; this.port = port; }
        public String toString() { return ip + ":" + port; }
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(CLIENT_PORT)) {
            System.out.println("Coordinator listening on port " + CLIENT_PORT);
            ExecutorService pool = Executors.newCachedThreadPool();
            while (true) {
                Socket client = serverSocket.accept();
                pool.submit(() -> handleClient(client));
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request;
            while ((request = in.readLine()) != null) {
                if (request.trim().isEmpty()) continue;
                String cmd = request.split(" ")[0].toLowerCase();

                switch (cmd) {
                    case "search":
                        String keyword = request.length() > 7 ? request.substring(7) : "";
                        List<String> results = broadcastSearch(keyword);
                        if (results.isEmpty()) out.println("No results");
                        else results.forEach(out::println);
                        out.println("END_OF_RESPONSE");
                        break;

                    case "lease":
                        String leaseId = request.length() > 6 ? request.substring(6).trim() : "";
                        out.println(forwardRequest(leaseId, "LEASE"));
                        out.println("END_OF_RESPONSE");
                        break;

                    case "return":
                        String returnId = request.length() > 7 ? request.substring(7).trim() : "";
                        out.println(forwardRequest(returnId, "RETURN"));
                        out.println("END_OF_RESPONSE");
                        break;

                    case "stats":
                        Map<String, Integer> stats = collectStats();
                        out.println("Stats aggregated: " + stats.toString());
                        out.println("END_OF_RESPONSE");
                        break;

                    case "list":
                        List<String> lists = collectList();
                        if (lists.isEmpty()) out.println("No books reported");
                        else lists.forEach(out::println);
                        out.println("END_OF_RESPONSE");
                        break;

                    case "quit":
                        out.println("Goodbye!");
                        out.println("END_OF_RESPONSE");
                        return;

                    default:
                        out.println("Unknown command");
                        out.println("END_OF_RESPONSE");
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("Coordinator client handler error: " + e.getMessage());
        }
    }

    private List<String> broadcastSearch(String keyword) {
        List<String> merged = new ArrayList<>();
        for (ServerInfo s : libraryServers) {
            try (Socket sock = new Socket(s.ip, s.port);
                 PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
            ) {
                out.println("SEARCH " + keyword);
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_RESPONSE")) break;
                    merged.add(s + " -> " + line);
                }
            } catch (IOException e) {
                merged.add(s + " -> unreachable");
            }
        }
        return merged;
    }

    private String forwardRequest(String bookId, String command) {
        for (ServerInfo s : libraryServers) {
            try (Socket sock = new Socket(s.ip, s.port);
                 PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
            ) {
                out.println(command + " " + bookId);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_RESPONSE")) break;
                    sb.append(line).append(" ");
                }
                String resp = sb.toString().trim();
                if (!resp.isEmpty()) return s + " -> " + resp;
            } catch (IOException ignored) {}
        }
        return "Failed: no server available for book " + bookId;
    }

    private Map<String, Integer> collectStats() {
        Map<String, Integer> total = new HashMap<>();
        for (ServerInfo s : libraryServers) {
            try (Socket sock = new Socket(s.ip, s.port);
                 PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
            ) {
                out.println("STATS");
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_RESPONSE")) break;
                    if (line.startsWith("STATS")) {
                        String[] parts = line.substring(6).trim().split("\\s+");
                        for (String p : parts) {
                            String[] kv = p.split("=");
                            if (kv.length == 2) {
                                String key = kv[0].trim();
                                int val = Integer.parseInt(kv[1].trim());
                                total.put(key, total.getOrDefault(key, 0) + val);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
        return total;
    }

    private List<String> collectList() {
        List<String> list = new ArrayList<>();
        for (ServerInfo s : libraryServers) {
            try (Socket sock = new Socket(s.ip, s.port);
                 PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
            ) {
                out.println("LIST");
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("END_OF_RESPONSE")) break;
                    list.add(s + " -> " + line);
                }
            } catch (IOException e) {
                list.add(s + " -> unreachable");
            }
        }
        return list;
    }

    public static void main(String[] args) throws IOException {
        new CoordinatorServer().start();
    }
}
