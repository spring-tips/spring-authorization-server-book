package bootiful.javareloaded.loom;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

class LoomTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int port = 9091;

    private final String contents;

    LoomTest() throws IOException {
        this.contents = new ClassPathResource("/data")
                .getContentAsString(Charset.defaultCharset());
    }

    @Test
    void loom() throws Exception {
        var loomDuration = this.doTest(() -> Executors.newVirtualThreadPerTaskExecutor());
        log.info("loom duration " + loomDuration);
    }

    private long doTest(Supplier<ExecutorService> executorSupplier)  throws Exception {
        var main = Executors.newCachedThreadPool();
        var requests = 5;
        var server = new Server(executorSupplier.get(), this.port);
        try {
            main.submit(server::start);
            Thread.sleep(1000);
            var elapsedTimeForAllRequests = this.sendRequests(
                    executorSupplier.get(), requests);
            System.out.println("elapsed time for all requests: " + elapsedTimeForAllRequests);

        }//
        finally {
            log.info("calling stop");
            server.stop();
        }

        return 0;
    }

    private long sendRequests(ExecutorService executor, int numberOfRequests) {
        try {
            var cdl = new CountDownLatch(numberOfRequests);
            var start = System.nanoTime();
            for (var i = 0; i < numberOfRequests; i++) {
                executor.submit(() -> this.sendRequest(cdl));
            }
            cdl.await();
            var stop = System.nanoTime();
            return stop - start;
        }//
        catch (Throwable t) {
            throw new RuntimeException("got an exception making a request", t);
        }
    }

    private void sendRequest(CountDownLatch countDownLatch) {

        try (var socket = new Socket("127.0.0.1", this.port);
             var out = new PrintWriter(socket.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.print (this.contents);
            out.write(-1);
            out.flush();
            var serverResponse = in.readLine();
            log.info("server says [" + serverResponse + "]");
            countDownLatch.countDown();
        }//
        catch (Throwable e) {
            log.error("got an Exception", e);
        }
    }

}
