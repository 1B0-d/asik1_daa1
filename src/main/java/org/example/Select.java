package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public final class Select {
    // Метрики
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

    // Публичный API: k-й порядковый статистик (0-based)
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

    // Внутренний рекурсивный (hi — исключительная граница)
    private static int select(int[] a, int lo, int hi, int k, int depth) {
        recursions++;
        if (depth > maxDepth) maxDepth = depth;

        int n = hi - lo;
        if (n <= 5) {
            Arrays.sort(a, lo, hi);
            copies += n;
            return a[lo + k];
        }

        // 1) Группы по 5, берём медианы
        int groups = (n + 4) / 5;
        int[] medians = new int[groups];
        for (int i = 0; i < groups; i++) {
            int start = lo + i * 5;
            int end = Math.min(start + 5, hi);
            Arrays.sort(a, start, end);
            copies += (end - start);
            medians[i] = a[start + (end - start) / 2];
        }

        // 2) Медиана медиан как pivot
        int pivot = select(medians, 0, groups, groups / 2, depth + 1);

        // 3) Трёхпутевое разбиение: < pivot | == pivot | > pivot
        int[] eq = partition3(a, lo, hi, pivot);
        int L = eq[0]; // начало блока == pivot
        int R = eq[1]; // конец блока == pivot

        int leftSize = L - lo;
        int midSize  = R - L + 1;

        if (k < leftSize) {
            return select(a, lo, L, k, depth + 1);
        } else if (k < leftSize + midSize) {
            return pivot; // k попал в блок значений == pivot
        } else {
            return select(a, R + 1, hi, k - leftSize - midSize, depth + 1);
        }
    }

    // Трёхпутевое разбиение (hi — исключительная граница).
    // Возвращает [lt, gt], где lt..gt включительно — все элементы == pivot.
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
                compares += 2; // прошло оба сравнения: == pivot
                i++;
            }
        }
        return new int[]{lt, gt};
    }
}
