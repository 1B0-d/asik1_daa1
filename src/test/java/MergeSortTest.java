
import org.example.MergeSort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;


class MergeSortTest {

    static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i-1] > a[i]) return false;
        return true;
    }

     static int[] randArr(int n, long seed) {
        Random r = new Random(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = r.nextInt(2_000_001) - 1_000_000;
        return a;
    }

    @Test
    void fast_random_sizes() {
        int[] sizes = {1_000, 5_000, 20_000};
        for (int t = 0; t < sizes.length; t++) {
            int n = sizes[t];
            int[] a = randArr(n, 4321L + t);
            MergeSort.sort(a);
            assertTrue(isSorted(a));
        }
    }

    @Test
    void edge_cases_and_patterns() {
        int[] a1 = {}; MergeSort.sort(a1); assertTrue(isSorted(a1));
        int[] a2 = {1}; MergeSort.sort(a2); assertTrue(isSorted(a2));

        int[] inc = new int[5000];
        for (int i = 0; i < inc.length; i++) inc[i] = i;
        MergeSort.sort(inc); assertTrue(isSorted(inc));

        int[] dec = new int[5000];
        for (int i = 0; i < dec.length; i++) dec[i] = dec.length - i;
        MergeSort.sort(dec); assertTrue(isSorted(dec));

        int[] dups = {5, -1, 5, 0, -1, 3, 3, 3, 0};
        MergeSort.sort(dups); assertTrue(isSorted(dups));
    }

    @Test
    void heavy_optional() {
        if (!Boolean.getBoolean("HEAVY")) return;
        int n = Integer.getInteger("MS_N", 100_000);
        int[] a = randArr(n, 888);
        MergeSort.sort(a);
        assertTrue(isSorted(a));

        MergeSort.saveMetricsCsv("ms_rand_" + n + "_seed888", n);
    }
    @Test
    void metrics_smoke() {
        int[] a = randArr(10_000, 888);
        MergeSort.sort(a);
        MergeSort.saveMetricsCsv("ms_rand_10k_seed888", a.length);
        assertTrue(isSorted(a));
    }
}