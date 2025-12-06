import java.io.*;
import java.util.*;

public class BookDatabase {
    private final List<Book> books = new ArrayList<>();

    public BookDatabase(String filename) throws Exception {
        loadFromFile(filename);
    }

    private void loadFromFile(String filename) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] p = line.split(";", -1);
            int id = Integer.parseInt(p[0].trim());
            String title = p[1].trim();
            String author = p[2].trim();
            boolean available = p[3].trim().equalsIgnoreCase("available");
            String[] keywords = p[4].trim().split(",");

            books.add(new Book(id, title, author, available, keywords));
        }
        br.close();
    }

    public synchronized List<Book> search(String keyword) {
        List<Book> res = new ArrayList<>();
        for (Book b : books) if (b.matches(keyword)) res.add(b);
        return res;
    }

    public synchronized boolean lease(int id) {
        for (Book b : books) {
            if (b.id == id) {
                if (b.available) { b.available = false; return true; }
                return false;
            }
        }
        return false;
    }

    public synchronized boolean returnBook(int id) {
        for (Book b : books) {
            if (b.id == id) {
                if (!b.available) { b.available = true; return true; }
                return false;
            }
        }
        return false;
    }

    public synchronized int countTotal() { return books.size(); }
    public synchronized int countAvailable() {
        int n = 0; for (Book b : books) if (b.available) n++; return n;
    }
}
