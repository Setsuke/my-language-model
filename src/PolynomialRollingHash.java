public class PolynomialRollingHash extends MyHashFunction {
    protected int doHash(String s) {
        long h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = (h * 256 + s.charAt(i)) % 1_000_000_007L;
        }
        return (int) h;
    }
}