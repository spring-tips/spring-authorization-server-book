package bootiful.authorizationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Set;

import static org.springframework.security.core.userdetails.User.withDefaultPasswordEncoder;

@SpringBootApplication
public class AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }

/*    @Bean
    UserDetailsService inMemoryUserDetailsManager() {
        var userBuilder = User.withDefaultPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withDefaultPasswordEncoder().roles("USER").username("jlong").password("password").build(),
                User.withDefaultPasswordEncoder().roles("USER", "ADMIN").username("rwinch").password("p@ssw0rd").build()
        );
    }*/

    @Bean
    UserDetailsService userDetailsService() {
        var builder = withDefaultPasswordEncoder();
        return new DumbestUserDetailsService(Set.of(
                builder.roles("USER").username("jlong").password("password").build(),
                builder.roles("USER", "ADMIN").username("rwinch").password("p@ssw0rd").build()));
    }

}


