public class MyLinkedObject {

    private String word;
    private int count;
    private MyLinkedObject next;

    public MyLinkedObject(String w) {
        this.word = w;
        this.count = 1;
        this.next = null;
    }

    // insert w keeping the order (safe for any input order)
    public void setWord(String w) {
        if (w == null || w.isEmpty()) {
            return;
        }

        // if w should be before this node, make w the new head in-place
        if (w.compareTo(this.word) < 0) {
            MyLinkedObject n = new MyLinkedObject(this.word);
            n.count = this.count;
            n.next  = this.next;

            this.word  = w;
            this.count = 1;
            this.next  = n;
            return;
        }

        // same word then increment
        if (this.word.equals(w)) {
            this.count++;
            return;
        }

        // no next then append at tail
        if (this.next == null) {
            this.next = new MyLinkedObject(w);
            return;
        }

        // insert between this and next if needed
        if (w.compareTo(this.next.word) < 0) {
            MyLinkedObject n = new MyLinkedObject(w);
            n.next = this.next;
            this.next = n;
            return;
        }

        // go next
        this.next.setWord(w);
    }

    public boolean isWord(String w) {
        if (this.word.equals(w)) {
            return true;
        }
        if (this.next != null) {
            return this.next.isWord(w);
        }
        return false;
    }

    // get the word count
    public int getCount(String w) {
        MyLinkedObject current = this;
        while (current != null) {
            if (current.word.equals(w)) {
                return current.count;
            }
            current = current.next;
        }
        return 0;
    }

    public String getWord() {
        return word;
    }
    public int getSelfCount() {
        return count;
    }
    public MyLinkedObject getNext() {
        return next;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        MyLinkedObject current = this;

        while (current != null) {
            sb.append(current.word)
                    .append("[")
                    .append(current.count)
                    .append("]");
            if (current.next != null) {
                sb.append(" => ");
            }
            current = current.next;
        }

        return sb.toString();
    }
}
