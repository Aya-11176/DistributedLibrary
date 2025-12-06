public class Book {
    public int id;
    public String title;
    public String author;
    public boolean available;
    public String[] keywords;

    public Book(int id, String title, String author, boolean available, String[] keywords) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.available = available;
        this.keywords = keywords;
    }

    public boolean matches(String keyword) {
        if (keyword == null) return false;
        String k = keyword.toLowerCase();
        if (title.toLowerCase().contains(k)) return true;
        if (author.toLowerCase().contains(k)) return true;
        for (String kw : keywords) if (kw.toLowerCase().contains(k)) return true;
        return false;
    }
}
