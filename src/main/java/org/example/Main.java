package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Simple CLI:
 *   --algo mergesort|quicksort|select|closest
 *   --n <size>                e.g., 10000
 *   --seed <seed>             e.g., 42
 *   --runs <count>            e.g., 3
 *   --k <index>               (only for select; defaults to n/2)
 *   --out <file>              (CSV output file; we append here. If an algorithm
 *                              also writes its own file, we duplicate the row here as well.)
 */

public final class Main {

    public static void main(String[] args) {
        Map<String, String> opt = parseArgs(args);

        String algo = opt.getOrDefault("algo", "all").toLowerCase(Locale.ROOT);

        int n       = parseInt(opt.getOrDefault("n", "10000"), 10000);
        long seed0  = parseLong(opt.getOrDefault("seed", "123"), 123L);
        int runs    = parseInt(opt.getOrDefault("runs", "1"), 1);
        String out  = opt.getOrDefault("out", "metrics.csv");
        Integer kOpt = opt.containsKey("k") ? parseInt(opt.get("k"), n/2) : null;

        for (int run = 0; run < runs; run++) {
            long seed = seed0 + run;
            switch (algo) {
                case "all" -> {
                    int k = (kOpt != null ? kOpt : n / 2);
                    runMergeSort(n, seed, out);
                    runQuickSort(n, seed, out);
                    runSelect(n, seed, k, out);
                    runClosest(n, seed, out);
                }
                case "mergesort" -> runMergeSort(n, seed, out);   // << исправлено название
                case "quicksort" -> runQuickSort(n, seed, out);
                case "select"    -> runSelect(n, seed, (kOpt != null ? kOpt : n / 2), out);
                case "closest"   -> runClosest(n, seed, out);
                default -> {
                    System.err.println("Unknown --algo: " + algo +
                            " (use mergesort|quicksort|select|closest|all)");
                    System.exit(2);
                }
            }

        }
        System.out.println("Done.");
    }

    // ---------- runners ----------

    private static void runMergeSort(int n, long seed, String out) {
        int[] a = randArr(n, seed);
        MergeSort.sort(a);
        if (!isSorted(a)) throw new AssertionError("MergeSort failed");
        String label = "ms_rand_" + n + "_seed" + seed;
        // класс сам пишет в metrics.csv
        MergeSort.saveMetricsCsv(label, n);
        // дублируем строку в выбранный --out
        appendOut(out, label, n,
                "compares", MergeSort.compares,
                "copies", MergeSort.copies,
                "merges", MergeSort.merges,
                "insertion_calls", MergeSort.insertionCalls,
                "max_depth", MergeSort.maxDepth,
                "time_ms", MergeSort.lastNanos / 1_000_000.0
        );
    }

    private static void runQuickSort(int n, long seed, String out) {
        int[] a = randArr(n, seed);
        QuickSort.sort(a);
        if (!isSorted(a)) throw new AssertionError("QuickSort failed");
        String label = "qs_rand_" + n + "_seed" + seed;

        long compares = getLongField(QuickSort.class, "compares");
        long swaps    = getLongField(QuickSort.class, "swaps");
        long pivots   = getLongField(QuickSort.class, "pivots");
        long recs     = getLongField(QuickSort.class, "recursions");
        long depth    = getLongField(QuickSort.class, "maxDepth");
        long nanos    = getLongField(QuickSort.class, "lastNanos");
        QuickSort.saveMetricsCsv(label, n);

        appendOut(out, label, n,
                "compares", compares,
                "swaps", swaps,
                "pivots", pivots,
                "recursions", recs,
                "max_depth", depth,
                "time_ms", nanos / 1_000_000.0
        );
    }

    private static void runSelect(int n, long seed, int k, String out) {
        int[] a = randArr(n, seed);
        int val = Select.select(a, k);

        int[] copy = a.clone();
        Arrays.sort(copy);
        if (val != copy[k]) throw new AssertionError("Select failed");

        String label = "select_rand_" + n + "_seed" + seed + "_k" + k;
        Select.saveMetricsCsv(label, n);
        appendOut(out, label, n,
                "compares", Select.compares,
                "copies", Select.copies,
                "recursions", Select.recursions,
                "max_depth", Select.maxDepth,
                "time_ms", Select.lastNanos / 1_000_000.0
        );
    }

    private static void runClosest(int n, long seed, String out) {
        ClosestPair.Point[] pts = randPts(n, seed);
        double d = ClosestPair.closest(pts);
        if (d < 0) throw new AssertionError("ClosestPair failed");

        String label = "closest_rand_" + n + "_seed" + seed;
        ClosestPair.saveMetricsCsv(label, n);
        appendOut(out, label, n,
                "compares", ClosestPair.compares,
                "copies", ClosestPair.copies,
                "max_depth", ClosestPair.maxDepth,
                "time_ms", ClosestPair.lastNanos / 1_000_000.0
        );
    }

    // ---------- utils ----------

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.startsWith("--")) {
                String key = s.substring(2);
                String val = (i+1 < args.length && !args[i+1].startsWith("--")) ? args[++i] : "true";
                m.put(key, val);
            }
        }
        return m;
    }

    private static int[] randArr(int n, long seed) {
        Random r = new Random(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = r.nextInt(2_000_001) - 1_000_000;
        return a;
    }

    private static ClosestPair.Point[] randPts(int n, long seed) {
        Random r = new Random(seed);
        ClosestPair.Point[] a = new ClosestPair.Point[n];
        for (int i = 0; i < n; i++)
            a[i] = new ClosestPair.Point(r.nextInt(2_000_001) - 1_000_000,
                    r.nextInt(2_000_001) - 1_000_000);
        return a;
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i-1] > a[i]) return false;
        return true;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }

    private static void appendOut(String out, String label, int n, Object... kv) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(out, true))) {
            StringBuilder sb = new StringBuilder(label).append(", n: ").append(n);
            for (int i = 0; i < kv.length; i += 2) {
                sb.append(", ").append(kv[i]).append(": ").append(kv[i + 1]);
            }
            pw.println(sb.toString());
        } catch (IOException e) {
            System.err.println("Cannot write to " + out + ": " + e.getMessage());
        }
    }

    private static long getLongField(Class<?> clazz, String name) {
        try {
            var f = clazz.getField(name);
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof Number num) return num.longValue();
        } catch (Throwable ignored) {}
        return -1L;
    }
}
