package bootiful.authorizationserver.keys;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.*;


@Configuration
class KeyConfiguration {

    // <.>
    @Bean
    TextEncryptor textEncryptor(
            @Value("${jwk.persistence.password}") String pw,
            @Value("${jwk.persistence.salt}") String salt) {
        return Encryptors.text(pw, salt);
    }

    // <.>
    @Bean
    OAuth2TokenGenerator<OAuth2Token> delegatingOAuth2TokenGenerator(
            JwtEncoder encoder,
            OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var generator = new JwtGenerator(encoder);
        generator.setJwtCustomizer(customizer);
        return new DelegatingOAuth2TokenGenerator(generator,
                new OAuth2AccessTokenGenerator(), new OAuth2RefreshTokenGenerator());
    }

    // <.>
    @Bean
    NimbusJwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}





