package bootiful.api;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Map;

@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}

@Controller
@ResponseBody
class MeHttpController {

    @GetMapping("/me")
    Map<String, String> principal() {
        var principal = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication().getName();
        return Map.of("name", principal);
    }
}

@Controller
@ResponseBody
class CustomerHttpController {

    private final CustomerRepository customerRepository;

    CustomerHttpController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers")
    Collection<Customer> customers() {
        return this.customerRepository.findAll();
    }
}

@Configuration
class EmailRequestsIntegrationFlowConfiguration {

    private final String destinationName = "emails";

    @Bean
    IntegrationFlow emailRequestsIntegrationFlow(AmqpTemplate template) {
        var outboundAmqpAdapter = Amqp
                .outboundAdapter(template)
                .routingKey(this.destinationName);
        return IntegrationFlow
                .from(requests())
                .transform(new ObjectToJsonTransformer())
                .handle(outboundAmqpAdapter)
                .get();
    }

    @Bean
    DirectChannelSpec requests() {
        return MessageChannels.direct();
    }
}

@Controller
@ResponseBody
class EmailController {

    private final MessageChannel requests;

    private final CustomerRepository repository;

    EmailController(CustomerRepository repository, MessageChannel requests) {
        this.requests = requests;
        this.repository = repository;
    }

    @PostMapping("/email")
    Map<String, Object> email(@AuthenticationPrincipal Jwt jwt, @RequestParam Integer customerId) {
        var token = jwt.getTokenValue();
        var message = MessageBuilder
                .withPayload(repository.findCustomerById(customerId))
                .setHeader("Authorization", token)
                .build();
        var sent = this.requests.send(message);
        return Map.of("sent", sent, "customerId", customerId);
    }
}

interface CustomerRepository extends ListCrudRepository<Customer, Integer> {

    Customer findCustomerById(Integer id);
}

record Customer(@Id Integer id, String name, String email) {
}