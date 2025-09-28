package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public final class Plotter {

    static final class Row {
        String label, algo;
        int n;
        Double timeMs;
        Integer maxDepth;
    }

    public static void main(String[] args) throws Exception {
        Path csv = Paths.get(args.length > 0 ? args[0] : "metrics.csv");
        if (!Files.exists(csv)) {
            System.err.println("metrics.csv not found at: " + csv.toAbsolutePath());
            return;
        }

        List<Row> rows = parse(csv);
        Path outDir = Paths.get("results");
        Files.createDirectories(outDir);

        // агрегируем: среднее по одинаковым n
        var timeSeries  = aggregate(rows, r -> r.timeMs);
        var depthSeries = aggregate(rows, r -> r.maxDepth == null ? null : r.maxDepth.doubleValue());

        // рисуем графики
        plot(timeSeries,  "time (ms)",  outDir.resolve("time_ms.png"));
        plot(depthSeries, "max depth",  outDir.resolve("depth.png"));

        // чистый CSV
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outDir.resolve("metrics_clean.csv")))) {
            pw.println("label,algo,n,time_ms,max_depth");
            for (Row r : rows) {
                pw.printf(Locale.US, "%s,%s,%d,%s,%s%n",
                        r.label, r.algo, r.n,
                        r.timeMs == null ? "" : String.format(Locale.US, "%.6f", r.timeMs),
                        r.maxDepth == null ? "" : r.maxDepth.toString());
            }
        }

        System.out.println("Wrote plots to " + outDir.toAbsolutePath());
    }

    // ---------- parsing ----------

    private static List<Row> parse(Path path) throws Exception {
        List<Row> out = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 2) continue;

            Row r = new Row();
            r.label = parts[0].trim();
            r.algo = inferAlgo(r.label);

            Map<String, String> kv = new HashMap<>();
            for (int i = 1; i < parts.length; i++) {
                String[] p = parts[i].split(":", 2);
                if (p.length == 2) {
                    kv.put(p[0].trim().toLowerCase(Locale.ROOT),
                            p[1].trim());
                }
            }

            // n / array size
            String nStr = firstNonNull(kv.get("n"), kv.get("array size"));
            if (nStr == null) continue;
            r.n = parseIntSafe(nStr);

            // time
            String tStr = firstNonNull(kv.get("time ms"), kv.get("time in ms"), kv.get("time_ms"));
            if (tStr != null) r.timeMs = parseDoubleSafe(tStr);

            // depth
            String dStr = firstNonNull(kv.get("max depth"), kv.get("max_depth"));
            if (dStr != null) r.maxDepth = parseIntSafe(dStr);

            out.add(r);
        }
        return out;
    }

    private static String inferAlgo(String label) {
        String s = label.toLowerCase(Locale.ROOT);
        if (s.startsWith("ms_")) return "mergesort";
        if (s.startsWith("qs_")) return "quicksort";
        if (s.startsWith("select_")) return "select";
        if (s.startsWith("closest_")) return "closest";
        return "unknown";
    }

    private static int parseIntSafe(String s) {
        String digits = s.replaceAll("[^0-9-]", "");
        return Integer.parseInt(digits);
    }

    private static double parseDoubleSafe(String s) {
        return Double.parseDouble(s.replace(",", "."));
    }

    private static String firstNonNull(String... xs) {
        for (String x : xs) if (x != null && !x.isEmpty()) return x;
        return null;
    }

    // ---------- aggregation ----------

    private static Map<String, TreeMap<Integer, Double>> aggregate(
            List<Row> rows, java.util.function.Function<Row, Double> get) {

        Map<String, Map<Integer, List<Double>>> tmp = new LinkedHashMap<>();
        for (Row r : rows) {
            if ("unknown".equals(r.algo)) continue;
            Double v = get.apply(r);
            if (v == null) continue;
            tmp.computeIfAbsent(r.algo, k -> new HashMap<>())
                    .computeIfAbsent(r.n, k -> new ArrayList<>())
                    .add(v);
        }

        Map<String, TreeMap<Integer, Double>> res = new LinkedHashMap<>();
        for (var e : tmp.entrySet()) {
            TreeMap<Integer, Double> m = new TreeMap<>();
            for (var e2 : e.getValue().entrySet()) {
                List<Double> lst = e2.getValue();
                double sum = 0;
                for (double x : lst) sum += x;
                m.put(e2.getKey(), sum / lst.size()); // среднее по runs
            }
            res.put(e.getKey(), m);
        }
        return res;
    }

    // ---------- plotting (pure AWT) ----------

    private static void plot(Map<String, TreeMap<Integer, Double>> series,
                             String yLabel, Path outPng) throws Exception {
        int W = 900, H = 520, L = 70, R = 20, T = 20, B = 60;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int x0 = L, x1 = W - R, y0 = H - B, y1 = T;

        // bounds
        int minN = Integer.MAX_VALUE, maxN = Integer.MIN_VALUE;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (var m : series.values()) {
            if (m.isEmpty()) continue;
            minN = Math.min(minN, m.firstKey());
            maxN = Math.max(maxN, m.lastKey());
            for (double v : m.values()) {
                minY = Math.min(minY, v);
                maxY = Math.max(maxY, v);
            }
        }
        if (minN == Integer.MAX_VALUE) {
            ImageIO.write(img, "png", outPng.toFile());
            g.dispose();
            return;
        }
        if (minY == maxY) { minY -= 1; maxY += 1; }

        // axes
        g.setColor(Color.BLACK);
        g.drawLine(x0, y0, x1, y0);
        g.drawLine(x0, y0, x0, y1);
        g.drawString("n", (x0 + x1) / 2, H - 25);
        g.drawString(yLabel, 8, (y0 + y1) / 2);

        // ticks
        int ticks = 5;
        for (int i = 0; i <= ticks; i++) {
            double t = i / (double) ticks;
            int x = x0 + (int) Math.round((x1 - x0) * t);
            int n = (int) Math.round(minN + (maxN - minN) * t);
            g.drawLine(x, y0, x, y0 + 4);
            g.drawString(String.valueOf(n), x - 12, y0 + 18);

            int y = y0 - (int) Math.round((y0 - y1) * t);
            double v = minY + (maxY - minY) * t;
            g.drawLine(x0 - 4, y, x0, y);
            g.drawString(String.format(Locale.US, "%.2f", v), 10, y + 4);
        }

        // palette
        Color[] colors = new Color[]{Color.BLUE, Color.RED, Color.GREEN.darker(), Color.ORANGE.darker(), Color.MAGENTA};
        int ci = 0;

        // legend
        int legendX = x0 + 10, legendY = y1 + 15;

        for (var e : series.entrySet()) {
            Color c = colors[ci++ % colors.length];
            g.setColor(c);

            int prevX = -1, prevY = -1;
            for (var p : e.getValue().entrySet()) {
                int n = p.getKey();
                double val = p.getValue();
                int x = x0 + (int) Math.round((x1 - x0) * ((n - minN) / (double) (maxN - minN)));
                int y = y0 - (int) Math.round((y0 - y1) * ((val - minY) / (maxY - minY)));
                if (prevX != -1) g.drawLine(prevX, prevY, x, y);
                g.fillOval(x - 3, y - 3, 6, 6);
                prevX = x; prevY = y;
            }
            // legend item
            g.fillRect(legendX, legendY - 8, 12, 4);
            g.setColor(Color.BLACK);
            g.drawString(e.getKey(), legendX + 18, legendY - 2);
            legendY += 16;
        }
        g.dispose();
        Files.createDirectories(outPng.getParent());
        ImageIO.write(img, "png", outPng.toFile());
    }
}
