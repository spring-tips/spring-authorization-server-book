package bootiful.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    RouterFunction<ServerResponse> httpEndpoints(CustomerRepository repository) {
        return route(
                GET("/customers"), request -> ServerResponse.ok().body(repository.findAll())
        );
    }
}

interface CustomerRepository extends ListCrudRepository<Customer, Integer> {

    Customer findCustomerById(Integer id);
}

record Customer(@Id Integer id, String name, String email) {
}