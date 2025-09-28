package org.example;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public final class MergeSort {
    private static final int CUTOFF = 24;

    public static long compares = 0;
    public static long copies = 0;
    public static int  merges = 0;
    public static int  insertionCalls = 0;
    public static int  maxDepth = 0;
    public static long lastNanos = 0;

    public static void resetMetrics() {
        compares = 0;
        copies = 0;
        merges = 0;
        insertionCalls = 0;
        maxDepth = 0;
        lastNanos = 0;
    }

    public static void saveMetricsCsv(String label, int n) {
        double ms = lastNanos / 1_000_000.0; // ns -> ms
        try (PrintWriter out = new PrintWriter(new FileWriter("metrics.csv", true))) {
            System.out.println("metrics.csv -> " + new java.io.File("metrics.csv").getAbsolutePath());
            out.println(
                    label
                            + ", array size: " + n
                            + ", compares: " + compares
                            + ", copies: " + copies
                            + ", merges: " + merges
                            + ", insertion calls: " + insertionCalls
                            + ", max depth: " + maxDepth
                            + ", time in ms: " + ms
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void sort(int[] a) {
        resetMetrics();
        if (a == null || a.length <= 1) return;
        int[] buf = new int[a.length];
        long t0 = System.nanoTime();
        sort(a, 0, a.length, buf, 1);
        lastNanos = System.nanoTime() - t0;
    }

    private static void sort(int[] a, int lo, int hi, int[] buf, int depth) {
        if (depth > maxDepth) maxDepth = depth;

        int n = hi - lo;
        if (n <= 1) return;

        if (n <= CUTOFF) {
            insertion(a, lo, hi);
            insertionCalls = insertionCalls + 1;
            return;
        }

        int mid = lo + (hi - lo) / 2;
        sort(a, lo, mid, buf, depth + 1);
        sort(a, mid, hi, buf, depth + 1);

        compares = compares + 1;
        if (a[mid - 1] <= a[mid]) return;

        merge(a, lo, mid, hi, buf);
        merges = merges + 1;
    }

    private static void merge(int[] a, int lo, int mid, int hi, int[] buf) {
        int i = lo;
        int j = mid;
        int k = 0;

        while (i < mid && j < hi) {
            compares = compares + 1;
            if (a[i] <= a[j]) {
                buf[k] = a[i];
                i = i + 1;
                k = k + 1;
            } else {
                buf[k] = a[j];
                j = j + 1;
                k = k + 1;
            }
            copies = copies + 1;
        }

        while (i < mid) {
            buf[k] = a[i];
            i = i + 1;
            k = k + 1;
            copies = copies + 1;
        }
        while (j < hi) {
            buf[k] = a[j];
            j = j + 1;
            k = k + 1;
            copies = copies + 1;
        }

        for (int t = 0; t < k; t = t + 1) {
            a[lo + t] = buf[t];
            copies = copies + 1;
        }
    }

    // insertion sort на подотрезке [lo, hi)
    private static void insertion(int[] a, int lo, int hi) {
        for (int i = lo + 1; i < hi; i = i + 1) {
            int x = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > x) {
                compares = compares + 1;
                a[j + 1] = a[j];
                copies = copies + 1;
                j = j - 1;
            }

            if (j >= lo) compares = compares + 1;
            a[j + 1] = x;
            copies = copies + 1;
        }
    }
}