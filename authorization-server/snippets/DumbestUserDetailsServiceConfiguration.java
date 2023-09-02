package bootiful.authorizationserver;

import org.springframework.context.annotation.Configuration;

@Configuration
class DumbestUserDetailsConfiguration {

    @Bean
    UserDetailsService userDetailsService() {
        var builder = withDefaultPasswordEncoder();
        return new DumbestUserDetailsService(Set.of(
                builder.roles("USER").username("jlong").password("password").build(),
                builder.roles("USER", "ADMIN").username("rwinch").password("p@ssw0rd").build())
        );
    }
}
