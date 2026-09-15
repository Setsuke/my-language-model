import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyLanguageModel extends JFrame {

    // UI load/show document, words-count, sort button, total/unique words
    private final JTextArea docArea = new JTextArea();
    private final DefaultTableModel vocabModel =
            new DefaultTableModel(new Object[]{"Word", "Count"}, 0);
    private final JTable vocabTable = new JTable(vocabModel);
    private final JTextArea warningsArea = new JTextArea(4, 10);

    private final JButton loadBtn = new JButton("Load...");
    private final JButton toggleOrderBtn = new JButton("Sort: Alphabetical");
    private final JTextField mField = new JTextField("250", 5); // hash table size

    private final JLabel totalLabel = new JLabel("Total words: 0");
    private final JLabel uniqueLabel = new JLabel("Unique words: 0");

    // hash stats per algorithm
    private final DefaultTableModel algoStatsModel =
            new DefaultTableModel(new Object[]{"Algorithm","Stdev","Min","Max","Non-empty"}, 0);
    private final JTable algoStatsTable = new JTable(algoStatsModel);

    // UI LM bigram-trigram generation
    private final JTextField seedField = new JTextField("you have", 24);
    private final JButton genBigramBtn = new JButton("Generate Bigram");
    private final JButton genTrigramBtn = new JButton("Generate Trigram");
    private final JTextArea outputArea = new JTextArea(4, 20);

    private final DefaultTableModel ngramStatsModel =
            new DefaultTableModel(new Object[]{"Table","Stdev","Min","Max"}, 0);
    private final JTable ngramStatsTable = new JTable(ngramStatsModel);

    // sort state
    private boolean sortByCount = false;

    // three tables for algorithm comparison
    private MyHashTable tableFirst;
    private MyHashTable tablePoly;
    private MyHashTable tableDJB2;

    // n-gram LM tables with DJB2Hash
    private MyHashTable uniLM;
    private MyHashTable biLM;
    private MyHashTable triLM;

    // warning
    private int warningCount = 0;
    private static final int WARNING_LIMIT = 500;

    public MyLanguageModel() {
        super("MyLanguageModel");

        // top bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("m:"));
        top.add(mField);
        top.add(loadBtn);
        top.add(toggleOrderBtn);
        top.add(totalLabel);
        top.add(uniqueLabel);

        // center: document and vocabulary
        JSplitPane centerTop = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(docArea),
                new JScrollPane(vocabTable)
        );
        centerTop.setResizeWeight(0.5);

        // Bottom-left: warnings
        warningsArea.setEditable(false);
        JPanel bottomLeft = new JPanel(new BorderLayout());
        bottomLeft.add(new JLabel("Warnings (tokens ignored):"), BorderLayout.NORTH);
        bottomLeft.add(new JScrollPane(warningsArea), BorderLayout.CENTER);

        // bottom-mid: generation controls and output
        outputArea.setEditable(false);
        JPanel genPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        genPanel.add(new JLabel("First few words:"));
        genPanel.add(seedField);
        genPanel.add(genBigramBtn);
        genPanel.add(genTrigramBtn);

        JPanel bottomMid = new JPanel(new BorderLayout());
        bottomMid.add(genPanel, BorderLayout.NORTH);
        bottomMid.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // bottom-right: algorithms and n-grams
        JPanel algoStatsPanel = new JPanel(new BorderLayout());
        algoStatsPanel.add(new JLabel("Hash Stats (per algorithm):"), BorderLayout.NORTH);
        algoStatsPanel.add(new JScrollPane(algoStatsTable), BorderLayout.CENTER);

        JPanel ngramStatsPanel = new JPanel(new BorderLayout());
        ngramStatsPanel.add(new JLabel("n-gram Hash Stats(DJB2):"), BorderLayout.NORTH);
        ngramStatsPanel.add(new JScrollPane(ngramStatsTable), BorderLayout.CENTER);

        JTabbedPane statsTabs = new JTabbedPane();
        statsTabs.addTab("Algorithms", algoStatsPanel);
        statsTabs.addTab("n-grams", ngramStatsPanel);

        JSplitPane bottomSplitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bottomLeft, bottomMid);
        bottomSplitLeft.setResizeWeight(0.33);

        JSplitPane centerBottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bottomSplitLeft, statsTabs);
        centerBottom.setResizeWeight(0.66);

        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerTop, centerBottom);
        center.setResizeWeight(0.58);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // actions
        loadBtn.addActionListener(this::onLoad);
        toggleOrderBtn.addActionListener(e -> {
            sortByCount = !sortByCount;
            toggleOrderBtn.setText(sortByCount ? "Sort: Count" : "Sort: Alphabetical");
            refreshVocab();
            updateTopTotals();
        });
        genBigramBtn.addActionListener(e -> onGenerate(false));
        genTrigramBtn.addActionListener(e -> onGenerate(true));

        // window
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 750);
        setLocationRelativeTo(null);
    }

    private void onLoad(ActionEvent e) {
        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = ch.getSelectedFile();
        try {
            // Reset UI
            docArea.setText(new String(Files.readAllBytes(f.toPath())));
            vocabModel.setRowCount(0);
            warningsArea.setText("");
            algoStatsModel.setRowCount(0);
            ngramStatsModel.setRowCount(0);
            outputArea.setText("");

            // build tables
            buildAllTablesOnePass(f);

            refreshVocab();
            updateTopTotals();
            rebuildAlgoStatsTable();
            rebuildNgramStatsTable();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildAllTablesOnePass(File file) throws Exception {
        int m = parseM(mField.getText().trim());

        // compare algorithms
        tableFirst = new MyHashTable(m, new FirstLetterHash());
        tablePoly  = new MyHashTable(m, new PolynomialRollingHash());
        tableDJB2  = new MyHashTable(m, new DJB2Hash());

        // LM with DJB2
        uniLM = new MyHashTable(m, new DJB2Hash());
        biLM  = new MyHashTable(m, new DJB2Hash());
        triLM = new MyHashTable(m, new DJB2Hash());

        // n-grams
        String prev1 = null, prev2 = null;

        // read blocks
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file), 1 << 16)) {
            String line;
            while ((line = br.readLine()) != null) {
                int n = line.length();
                StringBuilder tok = new StringBuilder(32);

                // tokenizador simple: a–z, apostrofe, punto
                for (int i = 0; i <= n; i++) {
                    char c = (i < n) ? Character.toLowerCase(line.charAt(i)) : ' ';
                    boolean ok = (c >= 'a' && c <= 'z') || c == '\'' || c == '.';

                    if (ok && c != ' ') {
                        tok.append(c);
                    } else {
                        if (tok.length() > 0) {
                            String t = tok.toString();
                            // validaciones simples (sin regex)
                            boolean valid =
                                    isLettersOnly(t) ||
                                            isApostropheWord(t) ||
                                            endsWithDotWord(t);

                            if (valid) {
                                // unigrams (3 hash funcs) + LM
                                tableFirst.put(t);
                                tablePoly.put(t);
                                tableDJB2.put(t);
                                uniLM.put(t);

                                // bigrams / trigrams
                                if (prev1 != null) biLM.put(prev1 + " " + t);
                                if (prev2 != null) triLM.put(prev2 + " " + prev1 + " " + t);

                                // avanza contexto
                                prev2 = prev1;
                                prev1 = t;
                            } else {
                                appendWarning(t);
                            }
                            tok.setLength(0);
                        }
                    }
                }
                // trata salto de línea como separador normal (ya manejado)
            }
        }
    }

    // helpers sin regex:
    private static boolean isLettersOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return s.length() > 0;
    }
    private static boolean isApostropheWord(String s) {
        // a'b o ab'c
        int pos = s.indexOf('\'');
        if (pos <= 0 || pos >= s.length() - 1) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == pos) continue;
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }
    private static boolean endsWithDotWord(String s) {
        if (s.length() < 2 || s.charAt(s.length() - 1) != '.') return false;
        for (int i = 0; i < s.length() - 1; i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }

    // extract valid words
    private List<String> tokenize(String doc) {
        List<String> out = new ArrayList<>();
        if (doc == null || doc.isBlank()) return out;
        // split document into lines, line breaks.
        for (String line : doc.split("\\R")) {
            // split lines into words by whitespace
            for (String raw : line.split("\\s+")) {
                String t = raw.trim().toLowerCase();
                if (t.isEmpty()) continue;

                if (t.matches(".*[^a-z'.].*")) {
                    appendWarning(t);
                    continue;
                }
                if (t.matches("^[a-z]+$") // letters only
                        || t.matches("^[a-z]+'[a-z]+$") // apostrophe
                        || t.matches("^[a-z]+\\.$")) { // ends with a dot
                    out.add(t);
                } else {
                    appendWarning(t);
                }
            }
        }
        return out;
    }
    // validate m
    private int parseM(String s) {
        try {
            int v = Integer.parseInt(s);
            return (v > 0) ? v : 250;
        } catch (Exception ignored) {
            return 250;
        }
    }

    // refresh vocabulary table when change sort type or load document
    private void refreshVocab() {
        if (tableFirst == null) return;

        // get data
        List<MyLinkedObject> data = sortByCount ? tableFirst.byCount() : tableFirst.alphabetical();

        Object[][] rows = new Object[data.size()][2];
        int i = 0;
        for (MyLinkedObject n : data) {
            rows[i][0] = n.getWord();
            rows[i][1] = n.getSelfCount();
            i++;
        }
        DefaultTableModel newModel = new DefaultTableModel(rows, new Object[]{"Word","Count"});
        vocabTable.setModel(newModel);
    }

    // add token to warning area
    private void appendWarning(String t) {
        if (warningCount < WARNING_LIMIT) {
            warningsArea.append(t);
            warningsArea.append("\n");
            warningCount++;
            if (warningCount == WARNING_LIMIT) {
                warningsArea.append("... (more ignored tokens omitted)\n");
            }
        }
    }

    // update total of words and unique words
    private void updateTopTotals() {
        if (tableFirst == null) return;
        totalLabel.setText("Total: " + tableFirst.totalCount());
        uniqueLabel.setText("Unique: " + tableFirst.uniqueCount());
    }

    // rebuild algorithms stats when load document
    private void rebuildAlgoStatsTable() {
        algoStatsModel.setRowCount(0);
        if (tableFirst == null || tablePoly == null || tableDJB2 == null) return;

        addAlgoStatsRow("FirstLetter", tableFirst);
        addAlgoStatsRow("Polynomial", tablePoly);
        addAlgoStatsRow("DJB2",       tableDJB2);
    }

    private void addAlgoStatsRow(String name, MyHashTable table) {
        int[] lens = table.groupLengths();

        int max = 0, nonEmpty = 0, min = Integer.MAX_VALUE;
        long sum = 0;
        for (int x : lens) {
            sum += x;
            if (x > 0) nonEmpty++;
            if (x > max) max = x;
            if (x < min) min = x;
        }
        if (lens.length == 0) min = 0;

        double mean = (lens.length == 0) ? 0 : (double) sum / lens.length;
        double var = 0;
        for (int x : lens) {
            double d = x - mean;
            var += d * d;
        }
        double stdev = (lens.length == 0) ? 0 : Math.sqrt(var / lens.length);

        algoStatsModel.addRow(new Object[] {
                name,
                String.format("%.2f", stdev),
                min == Integer.MAX_VALUE ? 0 : min,
                max,
                nonEmpty
        });
    }

    private void rebuildNgramStatsTable() {
        ngramStatsModel.setRowCount(0);
        if (uniLM == null || biLM == null || triLM == null) return;

        addNgramStatsRow("Unigrams", uniLM);
        addNgramStatsRow("Bigrams",  biLM);
        addNgramStatsRow("Trigrams", triLM);
    }

    private void addNgramStatsRow(String name, MyHashTable table) {
        int[] lens = table.groupLengths();

        int max = 0, nonEmpty = 0, min = Integer.MAX_VALUE;
        long sum = 0;
        for (int x : lens) {
            sum += x;
            if (x > 0) nonEmpty++;
            if (x > max) max = x;
            if (x < min) min = x;
        }
        if (lens.length == 0) min = 0;

        double mean = (lens.length == 0) ? 0 : (double) sum / lens.length;
        double var = 0;
        for (int x : lens) {
            double d = x - mean;
            var += d * d;
        }
        double stdev = (lens.length == 0) ? 0 : Math.sqrt(var / lens.length);

        ngramStatsModel.addRow(new Object[] {
                name,
                String.format("%.2f", stdev),
                min == Integer.MAX_VALUE ? 0 : min,
                max,
                nonEmpty
        });
    }

    // generation trigram
    private void onGenerate(boolean trigram) {
        if (uniLM == null || biLM == null) {
            JOptionPane.showMessageDialog(this, "Load a document first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String seed = seedField.getText();
        String out = trigram ? generateTrigram(seed, 20) : generateBigram(seed, 20);
        outputArea.setText(out);
    }

    // bigram generation using p(next|prev) = c(prev,next)/c(prev)
    private String generateBigram(String seed, int maxWords) {
        if (seed == null) seed = "";
        String[] parts = seed.trim().toLowerCase().split("\\s+");
        List<String> output = new ArrayList<>();
        for (String p : parts) if (!p.isBlank()) output.add(p);

        if (output.isEmpty()) {
            //if no seed, then it start with the most frequent unigram
            String start = mostFrequentUnigram();
            output.add(start);
        }

        String prev = output.get(output.size() - 1);
        for (int i = 0; i < maxWords; i++) {
            String next = argmaxNextByBigram(prev);
            if (next == null) break;
            output.add(next);
            prev = next;
        }
        return String.join(" ", output);
    }

    // trigram require at least two words in seed
    private String generateTrigram(String seed, int maxWords) {
        if (seed == null) seed = "";
        String[] parts = seed.trim().toLowerCase().split("\\s+");
        List<String> output = new ArrayList<>();
        for (String p : parts) if (!p.isBlank()) output.add(p);

        if (output.size() < 2) {
            return "(need at least two words for trigram generation)";
        }

        String w2 = output.get(output.size() - 2);
        String w1 = output.get(output.size() - 1);

        for (int i = 0; i < maxWords; i++) {
            String next = argmaxNextByTrigram(w2, w1);
            if (next == null) {
                output.add("(no trigram for context: " + w2 + " " + w1 + ")");
                break;
            }
            output.add(next);
            w2 = w1; w1 = next;
        }
        return String.join(" ", output);
    }

    private String mostFrequentUnigram() {
        List<MyLinkedObject> list = uniLM.byCount();
        return list.isEmpty() ? "the" : list.get(0).getWord();
    }

    private String argmaxNextByBigram(String prev) {
        double bestP = -1.0;
        String best = null;

        // iterate groups
        for (MyLinkedObject head : biLM.getGroups()) {       // <-- añade getter simple en MyHashTable
            for (MyLinkedObject cur = head; cur != null; cur = cur.getNext()) {
                String bg = cur.getWord(); // "w1 w2"
                int sp = bg.indexOf(' ');
                if (sp <= 0) continue;
                if (!bg.regionMatches(0, prev, 0, sp)) continue; // compara w1 rápido
                String w2 = bg.substring(sp + 1);
                double p = pBigram(prev, w2);
                if (p > bestP) { bestP = p; best = w2; }
            }
        }
        return best;
    }

    private String argmaxNextByTrigram(String w2, String w1) {
        double bestP = -1.0;
        String best = null;
        String prefix = w2 + " " + w1 + " ";

        for (MyLinkedObject head : triLM.getGroups()) {      // <-- usa el mismo getter
            for (MyLinkedObject cur = head; cur != null; cur = cur.getNext()) {
                String tg = cur.getWord(); // "a b c"
                if (!tg.startsWith(prefix)) continue;
                String c = tg.substring(prefix.length());
                double p = pTrigram(w2, w1, c);
                if (p > bestP) { bestP = p; best = c; }
            }
        }
        return best;
    }


    // p(next|prev)= c(prev,next) / c(prev)
    private double pBigram(String prev, String next) {
        int cPrev = uniLM.getCount(prev);
        if (cPrev == 0) return 0.0;
        int cPair = biLM.getCount(prev + " " + next);
        return (cPair <= 0) ? 0.0 : ((double) cPair) / cPrev;
    }

    // p(next|w2,w1)= c(w2,w1,next) / c(w2,w1)
    private double pTrigram(String w2, String w1, String next) {
        int cPrevPair = biLM.getCount(w2 + " " + w1);
        if (cPrevPair == 0) return 0.0;
        int cTrip = triLM.getCount(w2 + " " + w1 + " " + next);
        return (cTrip <= 0) ? 0.0 : ((double) cTrip) / cPrevPair;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MyLanguageModel().setVisible(true));
    }
}
