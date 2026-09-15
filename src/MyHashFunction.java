public abstract class MyHashFunction {

    public final int hash(String s, int m) {
        if (s == null) {
            s = "";
        }

        if (m <= 0) {
            throw new IllegalArgumentException("m must be > 0");
        }

        // normalization
        s = s.trim().toLowerCase();

        // call the hash algorithm from the subclass
        int h = doHash(s);

        // number valid between 0 and m-1
        h = h % m;
        if (h < 0) {
            h += m;
        }

        return h;
    }

    // each subclass determine the hash algorithm
    protected abstract int doHash(String s);
}
