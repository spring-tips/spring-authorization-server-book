package bootiful.javareloaded.loom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class LoomTest {

    @EnableAutoConfiguration
    @Configuration
    static class LoomApp {

        @Bean
        RouterFunction<ServerResponse> http() throws JsonProcessingException {
            var json = new ObjectMapper()
                    .writeValueAsString(Map.of("message", "Hello, world!"));
            return RouterFunctions
                    .route()
                    .GET("/hello", request -> ServerResponse.ok().body(json))
                    .build();
        }

        @Bean
        RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder.build();
        }

        static Runnable request(RestTemplate template, CountDownLatch countDownLatch, int count) {
            return () -> {
                var re = template.getForEntity("http://localhost:8080/hello", String.class);
                countDownLatch.countDown();
                Assertions.assertTrue(re.getStatusCode().is2xxSuccessful(), "the result for [" + count + "] should be 200");
            };
        }

        @Bean
        ApplicationRunner runner(RestTemplate template) {
            return args -> {
                var service = Executors.newVirtualThreadPerTaskExecutor();
                var count = 1_000;
                var countDownLatch = new CountDownLatch(count);
                for (var i = 0; i < count; i++)
                    service.submit(request(template, countDownLatch, i));
                Assertions.assertTrue(countDownLatch.await(1, TimeUnit.MINUTES));
                System.out.println("finished the test...");
            };
        }
    }

    @Test
    void tomcat() throws Exception {

        var sa = new SpringApplicationBuilder()
                .sources(LoomApp.class)
                .properties(Map.of("spring.threads.virtual.enabled", true))
                .run();


    }


}
