package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public final class Select {

    public static long compares = 0;
    public static long copies = 0;
    public static int  recursions = 0;
    public static int  maxDepth = 0;
    public static long lastNanos = 0;

    public static void resetMetrics() {
        compares = 0;
        copies = 0;
        recursions = 0;
        maxDepth = 0;
        lastNanos = 0;
    }

    public static void saveMetricsCsv(String label, int n) {
        double ms = lastNanos / 1_000_000.0;
        try (PrintWriter out = new PrintWriter(new FileWriter("metrics.csv", true))) {
            out.println(
                    label
                            + ", array size: " + n
                            + ", compares: " + compares
                            + ", copies: " + copies
                            + ", recursions: " + recursions
                            + ", max depth: " + maxDepth
                            + ", time in ms: " + ms
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int select(int[] a, int k) {
        resetMetrics();
        if (a == null || a.length == 0 || k < 0 || k >= a.length) {
            throw new IllegalArgumentException("Invalid input");
        }
        long t0 = System.nanoTime();
        int res = select(a, 0, a.length, k, 1);
        lastNanos = System.nanoTime() - t0;
        return res;
    }

    private static int select(int[] a, int lo, int hi, int k, int depth) {
        recursions++;
        if (depth > maxDepth) maxDepth = depth;

        int n = hi - lo;
        if (n <= 5) {
            Arrays.sort(a, lo, hi);
            copies += n;
            return a[lo + k];
        }

        int groups = (n + 4) / 5;
        int[] medians = new int[groups];
        for (int i = 0; i < groups; i++) {
            int start = lo + i * 5;
            int end = Math.min(start + 5, hi);
            Arrays.sort(a, start, end);
            copies += (end - start);
            medians[i] = a[start + (end - start) / 2];
        }
        int pivot = select(medians, 0, groups, groups / 2, depth + 1);

        int[] eq = partition3(a, lo, hi, pivot);
        int L = eq[0];
        int R = eq[1];

        int leftSize = L - lo;
        int midSize  = R - L + 1;

        if (k < leftSize) {
            return select(a, lo, L, k, depth + 1);
        } else if (k < leftSize + midSize) {
            return pivot;
        } else {
            return select(a, R + 1, hi, k - leftSize - midSize, depth + 1);
        }
    }

    private static int[] partition3(int[] a, int lo, int hi, int pivot) {
        int lt = lo, i = lo, gt = hi - 1;
        while (i <= gt) {
            if (a[i] < pivot) {
                compares++;
                int t = a[lt]; a[lt] = a[i]; a[i] = t; copies += 2;
                lt++; i++;
            } else if (a[i] > pivot) {
                compares += 2;
                int t = a[i]; a[i] = a[gt]; a[gt] = t; copies += 2;
                gt--;
            } else {
                compares += 2;
                i++;
            }
        }
        return new int[]{lt, gt};
    }
}
