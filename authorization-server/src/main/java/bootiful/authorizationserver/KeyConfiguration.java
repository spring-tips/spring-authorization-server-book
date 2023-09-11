package bootiful.authorizationserver;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class KeyConfiguration {

    private final String keyId;

    private final ApplicationEventPublisher publisher;

    @Bean
    RsaKeyPairRepositoryJWKSource rsaKeyPairRepositoryJWKSource(RsaKeyPairRepository repository) {
        return new RsaKeyPairRepositoryJWKSource(repository);
    }

    @Bean
    NimbusJwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtGenerator jwtGenerator(JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var generator = new JwtGenerator(jwtEncoder);
        generator.setJwtCustomizer(customizer);
        return generator;
    }

    @Bean
    InMemoryRsaKeyPairRepository repository() {
        return new InMemoryRsaKeyPairRepository();
    }


    KeyConfiguration(@Value("${jwk.key.id}") String keyId,
                    ApplicationEventPublisher publisher) {
        this.keyId = keyId;
        this.publisher = publisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (this.repository().findKeyPairs().isEmpty())
            this.publisher.publishEvent(new KeyPairGenerationRequest());
    }

    private RsaKeyPairRepository.RsaKeyPair generateKeyPair(Instant created) {
        var keyPair = generateRsaKey();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RsaKeyPairRepository.RsaKeyPair(this.keyId,
                created, publicKey, privateKey);
    }

    private KeyPair generateRsaKey() {
        var keyPair = (KeyPair) null;
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        }//
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @EventListener(KeyPairGenerationRequest.class)
    public void keyPairGenerationRequestListener()
            throws Exception {
        this.repository().save(generateKeyPair(Instant.now()));
    }
}

class InMemoryRsaKeyPairRepository implements RsaKeyPairRepository {

    private final Map<String, RsaKeyPair> idToKeyPair = new ConcurrentHashMap<>();

    @Override
    public List<RsaKeyPair> findKeyPairs() {
        return this.idToKeyPair
                .values()
                .stream()
                .sorted(Comparator.comparing(RsaKeyPair::created).reversed())
                .toList();

    }

    @Override
    public void delete(String id) {
        this.idToKeyPair.remove(id);
    }

    @Override
    public void save(RsaKeyPair rsaKeyPair) {
        this.idToKeyPair.put(rsaKeyPair.id(), rsaKeyPair);
    }

}


record KeyPairGenerationRequest() {
}

interface RsaKeyPairRepository {

    List<RsaKeyPair> findKeyPairs();

    void delete(String id);

    void save(RsaKeyPair rsaKeyPair);

    record RsaKeyPair(
            String id,
            Instant created,
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey) {
    }


}


class RsaKeyPairRepositoryJWKSource implements JWKSource<SecurityContext>,
        OAuth2TokenCustomizer<JwtEncodingContext> {

    private final RsaKeyPairRepository keyPairRepository;

    RsaKeyPairRepositoryJWKSource(RsaKeyPairRepository keyPairRepository) {
        this.keyPairRepository = keyPairRepository;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) throws KeySourceException {
        var keyPairs = this.keyPairRepository.findKeyPairs();
        var result = new ArrayList<JWK>(keyPairs.size());
        for (var keyPair : keyPairs) {
            var rsaKey = new RSAKey
                    .Builder(keyPair.publicKey())
                    .privateKey(keyPair.privateKey())
                    .keyID(keyPair.id())
                    .build();
            if (jwkSelector.getMatcher().matches(rsaKey)) {
                result.add(rsaKey);
            }
        }
        return result;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        var keyPairs = this.keyPairRepository.findKeyPairs();
        var kid = keyPairs.get(0).id();
        context.getJwsHeader().keyId(kid);
    }


}

