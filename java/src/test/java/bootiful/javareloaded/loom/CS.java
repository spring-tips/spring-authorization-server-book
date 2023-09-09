package bootiful.javareloaded.loom;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SpringBootApplication
public class CS {

    private final int port = 8080;
    private final Supplier<ExecutorService> executorServiceSupplier = () -> Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        SpringApplication.run(CS.class, args);
    }

    @Bean
    Server server(ApplicationEventPublisher publisher) {
        return new Server(publisher,
                this.executorServiceSupplier.get(), this.port);
    }


    @Bean
    Client client() {
        return new Client("127.0.0.1", this.port);
    }


    @Bean
    Launcher launcher(Client client, Server server) {
        return new Launcher(executorServiceSupplier.get(), server, client);
    }

    static class Launcher {

        private final Server server;
        private final Client client;
        private final ExecutorService executorService;

        Launcher(ExecutorService executorService, Server server, Client client) {
            this.server = server;
            this.client = client;
            this.executorService = executorService;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void serverRunner() throws Exception {
            this.executorService.submit(this.server::start);
        }

        @EventListener(ServerStartedEvent.class)
        public void clientRunner() {
            this.executorService.submit(this.client::connect);
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
                System.out.println("Received from server: " + response);

            }//
            catch (IOException e) {
                Utils.error(e);
            }
        }


    }


    record ServerStartedEvent() {
    }


    static class Server {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final ExecutorService executorService;
        private final int port;
        private final ApplicationEventPublisher publisher;

        Server(ApplicationEventPublisher publisher, ExecutorService executorService, int port) {
            this.executorService = executorService;
            this.publisher = publisher;
            this.port = port;
        }


        public void start() {
            this.running.set(true);
            try (var serverSocket = new ServerSocket(port)) {
                System.out.println("Server is listening on port " + port);
                publisher.publishEvent(new ServerStartedEvent());
                while (this.running.get()) {
                    var clientSocket = serverSocket.accept();
                    executorService.submit(new ClientRequestHandler(clientSocket));
                }
            }//
            catch (IOException e) {
                Utils.error(e);
            }
        }


        private static class ClientRequestHandler implements Runnable {

            private final Socket socket;

            ClientRequestHandler(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                try (var in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                     var out = new PrintWriter(this.socket.getOutputStream(), true)) {

                    System.out.println("Client connected.");

                    var input = in.readLine();
                    System.out.println("Received from client: " + input);

                    // Reverse the string and send it back
                    var reversed = new StringBuilder(input).reverse().toString();
                    out.println(reversed);
                }//
                catch (Throwable throwable) {
                    Utils.error(throwable);
                }//
                finally {
                    Utils.closeSocket(this.socket);
                }
            }

        }

    }

}

