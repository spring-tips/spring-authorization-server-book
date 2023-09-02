package bootiful.authorizationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SpringBootApplication
public class AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }

    @Bean
    UserDetailsService inMemoryUserDetailsManager() {
        var userBuilder = User.withDefaultPasswordEncoder();
        return new InMemoryUserDetailsManager(
                userBuilder.roles("USER").username("jlong").password("password").build(),
                userBuilder.roles("USER", "ADMIN").username("rwinch").password("p@ssw0rd").build()
        );
    }


}

