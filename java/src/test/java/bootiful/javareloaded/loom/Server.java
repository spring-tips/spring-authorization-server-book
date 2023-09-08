package bootiful.javareloaded.loom;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple reversing service. Use with: {@code  curl https://www.wikipedia.org/ | nc 127.0.0.1 9090 }
 */
class Server {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final int port;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    Server(ExecutorService executor, int port) {
        this.port = port;
        this.executor = executor;
    }

    private void handleRequest(Socket socket, InputStream in, OutputStream out) throws Throwable {
        try {
            var next = -1;
            var byteArrayOutputStream = new ByteArrayOutputStream();
            while ((next = in.read()) != -1)
                byteArrayOutputStream.write(next);
            var request = byteArrayOutputStream.toString();
            var reversedBytes = new StringBuilder(request)
                    .reverse()
                    .toString();
            out.write(reversedBytes.getBytes(StandardCharsets.UTF_8));

        } ///
        finally {
            socket.close();
        }
    }

    public void stop() {
        this.running.set(false);
    }

    public void start() {
        log.info("starting...");
        try {
            this.running.set(true);
            try (var serverSocket = new ServerSocket(this.port)) {
                while (this.running.get()) {
                    var client = serverSocket.accept();
                    var is = client.getInputStream();
                    var os = client.getOutputStream();
                    this.executor.submit(() -> {
                        try {
                            log.info("got a new request");
                            handleRequest(client, is, os);
                        }//
                        catch (Throwable exception) {
                            throw new RuntimeException("there's been an exception in our request handler: ",
                                    exception);
                        }
                    });
                }
            }
        }//
        catch (Throwable throwable) {
            log.error("got an exception", throwable);
        }

    }
}