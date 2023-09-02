package bootiful.authorizationserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Set;
import java.util.UUID;

@Configuration
class ClientsConfiguration {

    @Bean
    RegisteredClientRepository registeredClientRepository() {
        // <1>
        var registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("crm")
                .clientSecret("{bcrypt}$2a$10$m7dGi0viwVH63EjwZc6UdeUQxPuiVEEdFbZFI9nMxHAASTOIDlaVO")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantTypes(grantTypes -> grantTypes.addAll(Set.of(
                        AuthorizationGrantType.CLIENT_CREDENTIALS,
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN)))
                .redirectUri("http://127.0.0.1:8082/login/oauth2/code/spring")
                .scopes(scopes -> scopes.addAll(Set.of("user.read", "user.write", OidcScopes.OPENID)))
                .build();
        return new InMemoryRegisteredClientRepository(registeredClient);
    }
}
