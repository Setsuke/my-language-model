import java.util.*;

public class MyHashTable {

    private final MyHashFunction hashFunc;
    private final MyLinkedObject[] groups;
    private int totalWords = 0;
    private int uniqueWords = 0;

    public MyHashTable(int m, MyHashFunction hf) {
        if (m <= 0) {
            throw new IllegalArgumentException("m must be > 0");
        }
        if (hf == null) {
            throw new IllegalArgumentException("hash function is null");
        }
        this.hashFunc = hf;
        this.groups = new MyLinkedObject[m];
    }


    public void put(String w) {
        if (w == null){
            return;
        }
        w = w.trim().toLowerCase();
        if (w.isBlank()) {
            return;
        }
        int index = hashFunc.hash(w, groups.length);

        if (groups[index] == null) {
            groups[index] = new MyLinkedObject(w);
            uniqueWords++;
            totalWords++;
            return;
        }

        int before = groups[index].getCount(w);
        groups[index].setWord(w);
        int after = groups[index].getCount(w);

        if (before == 0 && after == 1) uniqueWords++;
        totalWords++;
    }

    public int getCount(String w) {
        if (w == null) return 0;
        w = w.trim().toLowerCase();
        if (w.isBlank()) return 0;

        int index = hashFunc.hash(w, groups.length);
        MyLinkedObject head = groups[index];
        return (head == null) ? 0 : head.getCount(w);
    }

    public int totalCount() {
        return totalWords;
    }

    public int uniqueCount() {
        return uniqueWords;
    }

    public int[] groupLengths() {
        int[] lens = new int[groups.length];
        for (int i = 0; i < groups.length; i++) {
            int len = 0;
            for (MyLinkedObject cur = groups[i]; cur != null; cur = cur.getNext()) {
                len++;
            }
            lens[i] = len;
        }
        return lens;
    }

    // return all MyLinkedObject nodes in a list
    private List<MyLinkedObject> collectAll() {
        List<MyLinkedObject> list = new ArrayList<>();
        for (MyLinkedObject head : groups) {
            for (MyLinkedObject cur = head; cur != null; cur = cur.getNext()) {
                list.add(cur);
            }
        }
        return list;
    }

    public List<MyLinkedObject> alphabetical() {
        List<MyLinkedObject> list = collectAll();
        list.sort((a, b) -> a.getWord().compareTo(b.getWord()));
        return list;
    }

    public List<MyLinkedObject> byCount() {
        List<MyLinkedObject> list = collectAll();
        list.sort((a, b) -> {
            int diff = b.getSelfCount() - a.getSelfCount();
            return (diff != 0) ? diff : a.getWord().compareTo(b.getWord());
        });
        return list;
    }

    public MyLinkedObject[] getGroups() { return groups; }

}
