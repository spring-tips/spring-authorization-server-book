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

@Controller
@ResponseBody
class EmailController {

    private record EmailSentStatus(String subject, Collection<String> scopes, Integer customerId, boolean sent) {
    }

    @PostMapping("/email")
    EmailSentStatus email(@AuthenticationPrincipal Jwt jwt, @RequestParam Integer customerId) {
        return new EmailSentStatus(
                jwt.getSubject(),
                (Collection<String>) jwt.getClaims().get("scope"),
                customerId, true
        );
    }
}

interface CustomerRepository extends ListCrudRepository<Customer, Integer> {
}

record Customer(@Id Integer id, String name, String email) {
}