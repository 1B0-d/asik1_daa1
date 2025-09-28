package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public final class ClosestPair {

    public static long compares = 0;
    public static long copies   = 0;
    public static int  maxDepth = 0;
    public static long lastNanos = 0;

    public static void resetMetrics() {
        compares = 0; copies = 0; maxDepth = 0; lastNanos = 0;
    }

    public static void saveMetricsCsv(String label, int n) {
        double ms = lastNanos / 1_000_000.0;
        try (PrintWriter out = new PrintWriter(new FileWriter("metrics.csv", true))) {
            out.println(label + ", n: " + n +
                    ", compares: " + compares +
                    ", copies: " + copies +
                    ", max depth: " + maxDepth +
                    ", time ms: " + ms);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static final class Point {
        public final int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    public static double closest(Point[] pts) {
        resetMetrics();
        if (pts == null || pts.length < 2) return 0.0;
        Point[] px = pts.clone();
        Arrays.sort(px, (a, b) -> a.x == b.x ? Integer.compare(a.y, b.y)
                : Integer.compare(a.x, b.x));
        Point[] tmp = new Point[px.length];
        long t0 = System.nanoTime();
        long best2 = rec(px, tmp, 0, px.length, 1);
        lastNanos = System.nanoTime() - t0;
        return Math.sqrt(best2);
    }
    private static long rec(Point[] a, Point[] tmp, int l, int r, int depth) {
        if (depth > maxDepth) maxDepth = depth;
        int n = r - l;
        if (n <= 3) {
            long best2 = Long.MAX_VALUE;
            for (int i = l; i < r; i++) {
                for (int j = i + 1; j < r; j++) {
                    long d2 = dist2(a[i], a[j]);
                    compares++;
                    if (d2 < best2) best2 = d2;
                }
            }

            Arrays.sort(a, l, r, (p, q) -> Integer.compare(p.y, q.y));
            return best2;
        }

        int m = (l + r) >>> 1;
        int midX = a[m].x;

        long left2  = rec(a, tmp, l, m, depth + 1);
        long right2 = rec(a, tmp, m, r, depth + 1);
        long best2  = Math.min(left2, right2);

        int i = l, j = m, k = l;
        while (i < m && j < r) {
            if (a[i].y <= a[j].y) { tmp[k++] = a[i++]; }
            else                   { tmp[k++] = a[j++]; }
            copies++;
        }
        while (i < m) { tmp[k++] = a[i++]; copies++; }
        while (j < r) { tmp[k++] = a[j++]; copies++; }
        for (k = l; k < r; k++) a[k] = tmp[k];
        Point[] strip = new Point[n];
        int sz = 0;
        for (int t = l; t < r; t++) {
            long dx = (long)a[t].x - midX;
            if (dx*dx < best2) strip[sz++] = a[t];
        }

        for (int p = 0; p < sz; p++) {
            for (int q = p + 1; q < sz && q <= p + 7; q++) {
                long dy = (long)strip[q].y - strip[p].y;
                if (dy*dy >= best2) { compares++; break; }
                long d2 = dist2(strip[p], strip[q]);
                compares++;
                if (d2 < best2) best2 = d2;
            }
        }
        return best2;
    }

    private static long dist2(Point a, Point b) {
        long dx = (long)a.x - b.x;
        long dy = (long)a.y - b.y;
        return dx*dx + dy*dy;
    }
}
