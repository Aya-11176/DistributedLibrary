import java.io.*;
import java.net.*;
import java.util.*;

public class LibraryServer {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java LibraryServer <port> <books-file>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String booksFile = args[1];

        try {
            BookDatabase db = new BookDatabase(booksFile);
            Statistics stats = new Statistics();

            ServerSocket server = new ServerSocket(port);
            System.out.println("Library Server started on port " + port);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());

                // Handle multiple commands per client
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    if (request.trim().isEmpty()) continue;

                    String[] parts = request.split(" ", 2);
                    String command = parts[0].toUpperCase();

                    switch (command) {
                        case "SEARCH":
                            String keyword = parts.length > 1 ? parts[1] : "";
                            stats.recordSearch(keyword);
                            List<Book> results = db.search(keyword);
                            if (results.isEmpty()) out.println("NOT_FOUND");
                            else {
                                for (Book b : results) {
                                    String safeTitle = b.title.replace(" ", "_");
                                    String status = b.available ? "available" : "not_available";
                                    out.println("FOUND " + b.id + " " + safeTitle + " " + status);
                                }
                            }
                            break;

                        case "LEASE":
                            try {
                                int id = Integer.parseInt(parts[1]);
                                if (db.lease(id)) { stats.recordLease(); out.println("LEASED"); }
                                else out.println("UNAVAILABLE");
                            } catch (Exception e) {
                                out.println("INVALID_COMMAND");
                            }
                            break;

                        case "RETURN":
                            try {
                                int idr = Integer.parseInt(parts[1]);
                                if (db.returnBook(idr)) { stats.recordReturn(); out.println("RETURNED"); }
                                else out.println("UNAVAILABLE");
                            } catch (Exception e) {
                                out.println("INVALID_COMMAND");
                            }
                            break;

                        case "STATS":
                            out.println(stats.getSummary());
                            break;

                        case "LIST":
                            List<Book> allBooks = db.search(""); // empty keyword returns all books
                            for (Book b : allBooks) {
                                String safeTitle = b.title.replace(" ", "_");
                                String status = b.available ? "available" : "not_available";
                                out.println(b.id + " " + safeTitle + " " + status);
                            }
                            break;

                        case "QUIT":
                            out.println("Goodbye!");
                            socket.close();
                            break;

                        default:
                            out.println("INVALID_COMMAND");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
