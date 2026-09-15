public class DJB2Hash extends MyHashFunction {
    protected int doHash(String s) {
        int h = 5381;
        for (int i = 0; i < s.length(); i++) {
            h = h * 33 + s.charAt(i);
        }
        return h;
    }
}