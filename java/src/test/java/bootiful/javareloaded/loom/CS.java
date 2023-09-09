package bootiful.javareloaded.loom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SpringBootApplication
public class CS {

    private final static Logger log = LoggerFactory.getLogger(CS.class.getName());

    private final int port = 8080;

    private final int requests = 100;

    private static ExecutorService loom() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    private static ExecutorService traditional() {
        return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CS.class, args);
        Thread.currentThread().join();
    }

    private final Supplier<ExecutorService> executorServiceSupplier = CS::loom;
    private final ExecutorService serviceExecutor = this.executorServiceSupplier.get();
    private final ExecutorService clientExecutor = this.executorServiceSupplier.get();

    @Bean
    Server server(ApplicationEventPublisher publisher) {
        return new Server(publisher, this.serviceExecutor,
                this.port);
    }


    @Bean
    Client client() {
        return new Client("127.0.0.1", this.port);
    }

    @Bean
    Launcher launcher(Client client, Server server) {
        return new Launcher(this.requests, this.clientExecutor, this.serviceExecutor, server, client);
    }

    static class Launcher {

        private final Server server;
        private final Client client;
        private final ExecutorService clientExecutor, serviceExecutor;
        private final CountDownLatch clientWaiter, serviceWaiter;
        private final int requests;

        Launcher(int requests, ExecutorService clientExecutor, ExecutorService serviceExecutor, Server server, Client client) {
            this.server = server;
            this.client = client;
            this.clientExecutor = clientExecutor;
            this.serviceExecutor = serviceExecutor;
            this.requests = requests;
            this.clientWaiter = new CountDownLatch(requests);
            this.serviceWaiter = new CountDownLatch(1);
        }

        @EventListener(ClientRequestHandledEvent.class)
        public void clientRequestHandledEventListener() {
            this.clientWaiter.countDown();
        }

        @EventListener(ApplicationReadyEvent.class)
        public void serverRunner() {
            this.serviceExecutor.submit(this.server::start);
        }

        @EventListener(ServerStartedEvent.class)
        public void clientRunner() {
            this.clientExecutor.submit(() -> {
                try {
                    var start = System.nanoTime();
                    for (var i = 0; i < this.requests; i++)
                        this.clientExecutor.submit(this.client::connect);
                    this.clientWaiter.await();
                    var stop = System.nanoTime();
                    this.server.stop();
                    log.info("duration: " + (stop - start));
                }//
                catch (InterruptedException e) {
                    Utils.error(e);
                }
            });
        }
    }


    static class Utils {

        static void closeSocket(Socket socket) {
            try {
                socket.close();
            }//
            catch (IOException e) {
                error(e);
            }
        }

        static void error(Throwable throwable) {
            LoggerFactory.getLogger(CS.class.getName()).error("oops!", throwable);
        }
    }

    static class Client {

        private final String serverAddress;
        private final int port;

        Client(String serverAddress, int port) {
            this.serverAddress = serverAddress;
            this.port = port;
        }

        public void connect() {
            try (var socket = new Socket(this.serverAddress, this.port);
                 var out = new PrintWriter(socket.getOutputStream(), true);
                 var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                var messageToSend = "HelloServer";
                out.println(messageToSend);

                var response = in.readLine();
                log.info("Received from server: " + response);
            }//
            catch (IOException e) {
                Utils.error(e);
            }
        }


    }


    record ClientRequestHandledEvent() {
    }

    record ServerStartedEvent() {
    }


    static class Server {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final ExecutorService executorService;
        private final ApplicationEventPublisher publisher;
        private final int port;

        Server(ApplicationEventPublisher publisher, ExecutorService executorService, int port) {
            this.executorService = executorService;
            this.publisher = publisher;
            this.port = port;
        }


        public void start() {
            this.running.set(true);
            try (var serverSocket = new ServerSocket(this.port)) {
                log.info("Server is listening on port " + this.port);
                publisher.publishEvent(new ServerStartedEvent());
                while (this.running.get()) {
                    log.info("waiting for the next socket");
                    var clientSocket = serverSocket.accept();
                    executorService.submit(new ClientRequestHandler(clientSocket, this.publisher));
                }

                log.info("stopping the server");
            }//
            catch (IOException e) {
                Utils.error(e);
            }
        }

        public void stop() {
            log.info("calling stop()");
            this.running.set(false);
        }


        private static class ClientRequestHandler implements Runnable {

            private final Socket socket;
            private final ApplicationEventPublisher publisher;

            ClientRequestHandler(Socket socket, ApplicationEventPublisher publisher) {
                this.socket = socket;
                this.publisher = publisher;
            }

            @Override
            public void run() {
                try (var in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                     var out = new PrintWriter(this.socket.getOutputStream(), true)) {

                    log.info("Client connected.");

                    var input = in.readLine();
                    log.info("Received from client: " + input);

                    // Reverse the string and send it back
                    var reversed = new StringBuilder(input).reverse().toString();
                    out.println(reversed);
                    publisher.publishEvent(new ClientRequestHandledEvent());
                }//
                catch (Throwable throwable) {
                    Utils.error(throwable);
                }//
                finally {
                    Utils.closeSocket(this.socket);
                }
                log.info("end of the request handler");
            }

        }

    }

}

