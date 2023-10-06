package bootiful.javareloaded.loom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.IntStream;

class LoomTest {

    @Test
    void threads() throws Exception {
        var switches = 5;
        var observed = new ConcurrentSkipListSet<String>();
        var threads = IntStream
                .range(0, 1000)// <.>
                .mapToObj(index -> Thread
                        .ofVirtual()// <.>
                        .unstarted(() -> {
                            for (var i = 0; i < switches; i++) // <.>
                                observed.addAll(observe(index));
                        }))
                .toList();
        for (var t : threads) t.start();
        for (var t : threads) t.join();
        Assertions.assertTrue(observed.size() > 1); // <.>
    }

    private static Set<String> observe(int index) {
        var before = Thread.currentThread().toString();
        try {
            Thread.sleep(100);
        }//
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        var after = Thread.currentThread().toString();
        return index == 0 ? new HashSet<>(Arrays.asList(before, after)) : Set.of();
    }
}
