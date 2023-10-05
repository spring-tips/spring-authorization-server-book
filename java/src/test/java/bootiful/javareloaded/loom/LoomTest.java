package bootiful.javareloaded.loom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.IntStream;

class LoomTest {

    private static Set<String> observe(int index) {
        var before = Thread.currentThread().toString();
        try {
            Thread.sleep(100);
        }//
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        var after = Thread.currentThread().toString();
        return index == 0 ? Set.of(before, after) : Set.of();
    }

    @Test
    void threads() throws Exception {

        var switches = 5;
        var observed = new ConcurrentSkipListSet<String>();

        var threads = IntStream
                .range(0, 1000)
                .mapToObj(index -> Thread
                        .ofVirtual()
                        .unstarted(() -> {

                            for (var i = 0; i < switches; i++)
                                observed.addAll(observe(index));

                        }))
                .toList();

        for (var t : threads) t.start();
        for (var t : threads) t.join();
        System.out.println(observed);
        Assertions.assertEquals(switches, observed.stream().filter(s -> !s.isEmpty()).toList().size());
    }

  /*  @EnableAutoConfiguration
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


    }*/


}
