import java.util.HashMap;

public class Statistics {
    private int totalSearches = 0;
    private int totalLeases = 0;
    private int totalReturns = 0;

    private final HashMap<String, Integer> keywordCount = new HashMap<>();

    public synchronized void recordSearch(String keyword) {
        totalSearches++;
        if (keyword == null) return;
        String k = keyword.toLowerCase();
        keywordCount.put(k, keywordCount.getOrDefault(k, 0) + 1);
    }

    public synchronized void recordLease() { totalLeases++; }
    public synchronized void recordReturn() { totalReturns++; }

    public synchronized String getSummary() {
        return "SEARCHES=" + totalSearches +
               ";LEASES=" + totalLeases +
               ";RETURNS=" + totalReturns +
               ";KEYWORDS=" + keywordCount.toString();
    }
}
