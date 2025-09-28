import org.example.Select;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;

class SelectTest {

    static int[] randArr(int n, long seed) {
        Random r = new Random(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = r.nextInt(2_000_001) - 1_000_000;
        return a;
    }

    @Test
    void small_examples() {
        int[] a = {5, 2, 9, 1, 7};
        assertEquals(1, Select.select(a.clone(), 0));
        assertEquals(2, Select.select(a.clone(), 1));
        assertEquals(5, Select.select(a.clone(), 2));
        assertEquals(7, Select.select(a.clone(), 3));
        assertEquals(9, Select.select(a.clone(), 4));
    }

    @Test
    void compare_with_sort() {
        int n = 200;
        int[] a = randArr(n, 123);
        for (int k = 0; k < n; k += 20) {
            int expected = Arrays.stream(a.clone()).sorted().toArray()[k];
            int actual = Select.select(a.clone(), k);
            assertEquals(expected, actual);
        }
    }

    @Test
    void metrics_smoke() {
        int[] a = randArr(1000, 555);
        int val = Select.select(a, 500);
        assertNotNull(val);
        Select.saveMetricsCsv("select_rand_1k_seed555", a.length);
    }
}
