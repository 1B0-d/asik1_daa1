import org.example.ClosestPair;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;

class ClosestPairTest {

    private static ClosestPair.Point p(int x, int y) { return new ClosestPair.Point(x,y); }

    private static ClosestPair.Point[] randPts(int n, long seed) {
        Random r = new Random(seed);
        ClosestPair.Point[] a = new ClosestPair.Point[n];
        for (int i = 0; i < n; i++)
            a[i] = new ClosestPair.Point(r.nextInt(2_000_001)-1_000_000,
                    r.nextInt(2_000_001)-1_000_000);
        return a;
    }

    private static double brute(ClosestPair.Point[] a) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++)
            for (int j = i+1; j < a.length; j++) {
                double dx = a[i].x - a[j].x;
                double dy = a[i].y - a[j].y;
                double d  = Math.hypot(dx, dy);
                if (d < best) best = d;
            }
        return best == Double.POSITIVE_INFINITY ? 0.0 : best;
    }

    @Test
    void small_examples() {
        assertEquals(5.0, ClosestPair.closest(new ClosestPair.Point[]{
                p(0,0), p(3,4), p(10,10)
        }), 1e-9);

        assertEquals(0.0, ClosestPair.closest(new ClosestPair.Point[]{
                p(7,7), p(7,7), p(-5,1)
        }), 1e-9);
    }

    @Test
    void compare_with_bruteforce() {
        ClosestPair.Point[] pts = randPts(800, 123);
        double got = ClosestPair.closest(pts);
        double exp = brute(pts);
        assertEquals(exp, got, 1e-9);
    }

    @Test
    void metrics_smoke() {
        ClosestPair.Point[] pts = randPts(2000, 555);
        double d = ClosestPair.closest(pts);
        assertTrue(d >= 0.0);
        ClosestPair.saveMetricsCsv("closest_rand_2k_seed555", pts.length);
    }
}
