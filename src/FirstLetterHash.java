public class FirstLetterHash extends MyHashFunction {
    protected int doHash(String s) {
        if (s.isEmpty()) return 0;
        return s.charAt(0);
    }
}
