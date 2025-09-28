package org.example;;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;

public final class QuickSort {

    public static long compares = 0;
    public static long swaps    = 0;
    public static long pivots   = 0;
    public static long recursions = 0;
    public static int  maxDepth = 0;
    public static long lastNanos = 0;

    public static void resetMetrics() {
        compares = swaps = pivots = recursions = 0;
        maxDepth = 0;
        lastNanos = 0;
    }


    public static void saveMetricsCsv(String label, int n) {
        double ms = lastNanos / 1_000_000.0;
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter("metrics.csv", true))) {
            out.println(
                    label
                            + ", array size: " + n
                            + ", compares: " + compares
                            + ", swaps: " + swaps
                            + ", pivots: " + pivots
                            + ", recursions: " + recursions
                            + ", max depth: " + maxDepth
                            + ", time in ms: " + ms
            );
        } catch (java.io.IOException e) { e.printStackTrace(); }
    }



    public static void sort(int[] a) {
        resetMetrics();
        long t0 = System.nanoTime();
        if (a != null && a.length > 1) quick(a, 0, a.length - 1, 1);
        lastNanos = System.nanoTime() - t0;
    }


    private static void quick(int[] a, int lo, int hi, int depth) {

        if (depth > maxDepth) maxDepth = depth;

        while (lo < hi) {

            int pivotIndex = ThreadLocalRandom.current().nextInt(lo, hi + 1);
            pivots++;
            swap(a, pivotIndex, hi);
            int pivot = a[hi];

            int i = lo;
            int j = lo;
            while (j < hi) {
                compares++;
                if (a[j] <= pivot) {
                    swap(a, i, j);
                    i++;
                }
                j++;
            }
            swap(a, i, hi);
            int p = i;

            int leftSize  = p - lo;
            int rightSize = hi - p;

            if (leftSize < rightSize) {

                if (lo < p - 1) { recursions++; quick(a, lo, p - 1, depth + 1); }
                lo = p + 1;
            } else {
                if (p + 1 < hi) { recursions++; quick(a, p + 1, hi, depth + 1); }
                hi = p - 1;
            }
        }
    }

    private static void swap(int[] a, int i, int j) {
        if (i == j) return;
        swaps++;
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }
}