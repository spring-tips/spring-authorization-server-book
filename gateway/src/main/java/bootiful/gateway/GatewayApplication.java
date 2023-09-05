package bootiful.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize.anyExchange().authenticated())//<1>
                .csrf(ServerHttpSecurity.CsrfSpec::disable)// <2>
                .oauth2Login(Customizer.withDefaults())//<3>
                .oauth2Client(Customizer.withDefaults()); // <4>
        return http.build();
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        var apiPrefix = "/api/";
        return rlb
                .routes()
                .route(rs -> rs
                        .path(apiPrefix + "**")
                        .filters(f -> f
                                .tokenRelay()
                                .rewritePath(apiPrefix + "(?<segment>.*)", "/$\\{segment}")
                        )
                        .uri("http://localhost:8081"))
                .route(rs -> rs
                        .path("/**")
                        .uri("http://localhost:8020")
                )
                .build();
    }
}
