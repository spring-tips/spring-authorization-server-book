package bootiful.javareloaded.loom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class LoomTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int port = 9091;

    private final String contents;

    private final String contentsReversed;

    private final byte[] contentsByteArray;

    LoomTest() throws IOException {
        this.contents = new ClassPathResource("/data")
                .getContentAsString(Charset.defaultCharset());
        this.contentsByteArray = this.contents.getBytes(StandardCharsets.UTF_8);
        this.contentsReversed = new StringBuilder(this.contents)
                .reverse()
                .toString();

    }

    @Test
    void loom() throws Exception {
        var loomDuration = this.doTest(() -> Executors.newVirtualThreadPerTaskExecutor());
        log.info("loom duration " + loomDuration);
//        var traditionalDuration = this.test(() -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
//        System.out.println("loom %s; traditional %s".formatted(loomDuration, traditionalDuration));
//        Assertions.assertTrue(traditionalDuration > loomDuration);
    }

    static class WaitingServer extends Server {

        private final CountDownLatch countDownLatch;

        WaitingServer(int port, ExecutorService executor, CountDownLatch waiter) {
            super(executor, port);
            this.countDownLatch = waiter;
        }

        @Override
        public void start() {
            super.start();
            this.countDownLatch.countDown();
        }

    }

    private long doTest(Supplier<ExecutorService> executorSupplier) throws Exception {
        var main = Executors.newCachedThreadPool();
        var wait = new CountDownLatch(1);
        var server = new WaitingServer(this.port, executorSupplier.get(), wait);
        try {
            main.submit(server::start);

            main.submit(() -> {
                var elapsedTimeForAllRequests = this.load(executorSupplier.get(), 1);
                System.out.println("elapsed time for all requests: " + elapsedTimeForAllRequests);
            });
            Thread.sleep(10_000);
        }//
        finally {
            log.info("calling stop");
            server.stop();
        }

        return 0;
    }

    private long load(ExecutorService executor, int numberOfRequests) {
        try {
            var cdl = new CountDownLatch(numberOfRequests);
            var start = System.nanoTime();
            for (var i = 0; i < numberOfRequests; i++) {
                executor.submit(() -> request(cdl ));
            }
            cdl.await();
            var stop = System.nanoTime();
            return stop - start;
        }//
        catch (Throwable t) {
            throw new RuntimeException("got an exception making a request", t);
        }
    }

    private boolean request(CountDownLatch countDownLatch) {
        try (var socket = new Socket("127.0.0.1", this.port)) {
            try (var out = new OutputStreamWriter(socket.getOutputStream())) {
                out.write(this.contents);
            }
            try (var in = socket.getInputStream()) {
                var reversedString = new String(
                        FileCopyUtils.copyToByteArray(in));
                log.info("got a reply [" + reversedString + "]");
                Assertions.assertEquals(reversedString,
                        this.contentsReversed,
                        "the string sent in should be reversed ");
                countDownLatch.countDown();


                return true;
            }

        } //
        catch (Throwable throwable) {
            System.out.println("got an error " + throwable.getMessage());
        }

        return false;
    }

}
