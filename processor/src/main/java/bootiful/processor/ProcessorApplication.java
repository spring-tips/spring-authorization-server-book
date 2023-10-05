package bootiful.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }

    public static final String RABBITMQ_DESTINATION_NAME = "emails";

    public static final String AUTHORIZATION_HEADER_NAME = "jwt";

}


