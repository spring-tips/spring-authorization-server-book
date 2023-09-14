package bootiful.authorizationserver.keys;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;


@Configuration
class KeyConfiguration {

    private final String keyId;

    KeyConfiguration(@Value("${jwk.key.id}") String keyId) {
        this.keyId = keyId;
    }

    private RsaKeyPairRepository.RsaKeyPair generateKeyPair(Instant created) {
        var keyPair = generateRsaKey();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RsaKeyPairRepository.RsaKeyPair(this.keyId, created, publicKey, privateKey);
    }

    private KeyPair generateRsaKey() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        }//
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    ApplicationListener<RsaKeyPairGenerationRequestEvent> keyPairGenerationRequestListener(
            RsaKeyPairRepository repository) {
        return event -> repository.save(generateKeyPair(event.getSource()));
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> applicationReadyListener(
            ApplicationEventPublisher publisher, RsaKeyPairRepository repository) {
        return event -> {
            if (repository.findKeyPairs().isEmpty())
                publisher.publishEvent(new RsaKeyPairGenerationRequestEvent(Instant.now()));
        };
    }

    @Bean
    NimbusJwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> delegatingOAuth2TokenGenerator(JwtEncoder encoder, OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var generator = new JwtGenerator(encoder);
        generator.setJwtCustomizer(customizer);
        return new DelegatingOAuth2TokenGenerator(generator,
                new OAuth2AccessTokenGenerator(), new OAuth2RefreshTokenGenerator());
    }
}





