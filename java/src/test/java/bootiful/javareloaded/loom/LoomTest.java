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
                .range(0, 1000)
                .mapToObj(index -> Thread
                        .ofVirtual()// <1>
                        .unstarted(() -> {
                            for (var i = 0; i < switches; i++) // <2>
                                observed.addAll(observe(index));
                        }))
                .toList();

        for (var t : threads) t.start(); // <3>

        for (var t : threads) t.join(); // <4>

        Assertions.assertTrue(observed.size() > 1); // <5>
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
        return index == 0 ? carefulAdd(before, after) : Set.of();
    }

    static private Set<String> carefulAdd(String... args) {
        return new HashSet<>(Arrays.asList(args));
    }
}
