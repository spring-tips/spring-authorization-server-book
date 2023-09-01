package bootiful.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.List;
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

@Controller
@ResponseBody
class EmailController {

    @GetMapping("/email")
    Map<String, Object> email(@AuthenticationPrincipal Jwt token, @RequestParam Integer customerId) {
        debug(token);
        return Map.of("customerId", customerId, "sent", true);
    }

    private static void debug(Jwt token) {
        System.out.println("--------------------");
        System.out.println(token.getSubject());
        var scopes = (List<String>) token.getClaims().get("scope");
        scopes.forEach(System.out::println);
    }
}

interface CustomerRepository extends ListCrudRepository<Customer, Integer> {
}

record Customer(@Id Integer id, String name, String email) {
}