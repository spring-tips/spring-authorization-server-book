package bootiful.authorizationserver;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;


@Configuration
class KeyConfiguration {

    private final String keyId;

    KeyConfiguration(@Value("${jwk.key.id}") String keyId) {
        this.keyId = keyId;
    }

    @Bean
    RsaKeyPairSerde keySerde() {
        return new RsaKeyPairSerde();
    }

    @Bean
    RsaKeyPairRepositoryJWKSource rsaKeyPairRepositoryJWKSource(RsaKeyPairRepository repository) {
        return new RsaKeyPairRepositoryJWKSource(repository);
    }

    @Bean
    NimbusJwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> delegatingOAuth2TokenGenerator(JwtEncoder encoder, OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var jg = this.jwtGenerator(encoder, customizer);
        return new DelegatingOAuth2TokenGenerator(jg, new OAuth2AccessTokenGenerator(), new OAuth2RefreshTokenGenerator());
    }

    private JwtGenerator jwtGenerator(JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var generator = new JwtGenerator(jwtEncoder);
        generator.setJwtCustomizer(customizer);
        return generator;
    }

    private RsaKeyPairRepository.RsaKeyPair generateKeyPair(Instant created) {
        var keyPair = generateRsaKey();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RsaKeyPairRepository.RsaKeyPair(this.keyId, created, publicKey, privateKey);
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

    @Bean
    ApplicationListener<RsaKeyPairGenerationRequestEvent> keyPairGenerationRequestListener(RsaKeyPairRepository repository) {
        return event -> repository.save(generateKeyPair(event.getSource()));
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> applicationReadyListener(ApplicationEventPublisher publisher, RsaKeyPairRepository repository) {
        return event -> {
            if (repository.findKeyPairs().isEmpty())
                publisher.publishEvent(new RsaKeyPairGenerationRequestEvent(Instant.now()));
        };
    }

    @Bean
    JdbcRsaKeyPairRepository jdbcRsaKeyPairRepository(RowMapper<RsaKeyPairRepository.RsaKeyPair> keyPairRowMapper, JdbcTemplate template, RsaKeyPairSerde serde) {
        return new JdbcRsaKeyPairRepository(keyPairRowMapper, template, serde);
    }

    @Bean
    RsaKeyPairRowMapper keyPairRowMapper(RsaKeyPairSerde serde) {
        return new RsaKeyPairRowMapper(serde);
    }
}


class RsaKeyPairRowMapper implements RowMapper<RsaKeyPairRepository.RsaKeyPair> {

    private final RsaKeyPairSerde serde;

    RsaKeyPairRowMapper(RsaKeyPairSerde serde) {
        this.serde = serde;
    }

    @Override
    public RsaKeyPairRepository.RsaKeyPair mapRow(ResultSet rs, int rowNum) throws SQLException {
        var publicKey = serde.publicKeys().deserialize(rs.getString("public_key"));
        var privateKey = serde.privateKeys().deserialize(rs.getString("private_key"));
        return new RsaKeyPairRepository.RsaKeyPair(rs.getString("id"), new Date(rs.getDate("created").getTime()).toInstant(), publicKey, privateKey);
    }
}

class JdbcRsaKeyPairRepository implements RsaKeyPairRepository {

    private final JdbcTemplate template;
    private final RsaKeyPairSerde serde;
    private final RowMapper<RsaKeyPair> keyPairRowMapper;

    JdbcRsaKeyPairRepository(RowMapper<RsaKeyPair> keyPairRowMapper, JdbcTemplate template, RsaKeyPairSerde rsaKeyPairSerde) {
        this.template = template;
        this.serde = rsaKeyPairSerde;
        this.keyPairRowMapper = keyPairRowMapper;

    }

    @Override
    public List<RsaKeyPair> findKeyPairs() {
        return this.template.query("select * from rsa_key_pairs order by created desc", this.keyPairRowMapper);
    }

    @Override
    public void delete(String id) {
        var updated = this.template.update("delete from rsa_key_pairs where id =?", id);
        Assert.state(updated == 0 || updated == 1, "no more than one row should've been affected");
    }

    @Override
    public void save(RsaKeyPair keyPair) {
        var sql = """
                INSERT INTO rsa_key_pairs (id, private_key, public_key, created)
                VALUES (?, ?, ?, ?)
                on conflict  on constraint rsa_key_pairs_id_created_key
                do nothing
                """;
        var publicKey = this.serde.publicKeys().serialize(keyPair.publicKey());
        var privateKey = this.serde.privateKeys().serialize(keyPair.privateKey());
        var updated = this.template.update(sql, keyPair.id(), privateKey, publicKey, new Date(keyPair.created().toEpochMilli()));
        Assert.state(updated == 0 || updated == 1, "no more than one record should have been updated");
    }
}


class RsaKeyPairGenerationRequestEvent extends ApplicationEvent {

    public RsaKeyPairGenerationRequestEvent(Instant instant) {
        super(instant);
    }

    @Override
    public Instant getSource() {
        return (Instant) super.getSource();
    }
}

interface RsaKeyPairRepository {

    List<RsaKeyPair> findKeyPairs();

    void delete(String id);

    void save(RsaKeyPair rsaKeyPair);

    record RsaKeyPair(String id, Instant created, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    }
}


class RsaKeyPairRepositoryJWKSource implements JWKSource<SecurityContext>, OAuth2TokenCustomizer<JwtEncodingContext> {

    private final RsaKeyPairRepository keyPairRepository;

    RsaKeyPairRepositoryJWKSource(RsaKeyPairRepository keyPairRepository) {
        this.keyPairRepository = keyPairRepository;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) throws KeySourceException {
        var keyPairs = this.keyPairRepository.findKeyPairs();
        var result = new ArrayList<JWK>(keyPairs.size());
        for (var keyPair : keyPairs) {
            var rsaKey = new RSAKey.Builder(keyPair.publicKey()).privateKey(keyPair.privateKey()).keyID(keyPair.id()).build();
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


class RsaKeyPairSerde {


    static class Public {

        public String serialize(RSAPublicKey publicKey) {
            var x509EncodedKeySpec = new java.security.spec.X509EncodedKeySpec(publicKey.getEncoded());
            return "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(x509EncodedKeySpec.getEncoded()) + "\n-----END PUBLIC KEY-----";
        }

        public RSAPublicKey deserialize(String pem) {
            try {
                String publicKeyPEM = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
                byte[] encoded = Base64.getMimeDecoder().decode(publicKeyPEM);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
                return (RSAPublicKey) keyFactory.generatePublic(keySpec);
            }//
            catch (Throwable throwable) {
                throw new IllegalArgumentException("there's been an exception", throwable);
            }
        }

    }

    static class Private {

        public String serialize(RSAPrivateKey privateKey) {
            var pkcs8EncodedKeySpec = new java.security.spec.PKCS8EncodedKeySpec(privateKey.getEncoded());
            return "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded()) + "\n-----END PRIVATE KEY-----";
        }

        public RSAPrivateKey deserialize(String pem) {
            try {
                var privateKeyPEM = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
                var encoded = Base64.getMimeDecoder().decode(privateKeyPEM);
                var keyFactory = KeyFactory.getInstance("RSA");
                var keySpec = new PKCS8EncodedKeySpec(encoded);
                return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            }//
            catch (Throwable throwable) {
                throw new IllegalArgumentException("there's been an exception", throwable);
            }
        }
    }

    Public publicKeys() {
        return new Public();
    }

    Private privateKeys() {
        return new Private();
    }
}

class RsaPrivateKeySerializer implements Serializer<RSAPrivateKey>,
        Deserializer<RSAPrivateKey> {

    private final RsaKeyPairSerde serde = new RsaKeyPairSerde();

    @Override
    public RSAPrivateKey deserialize(InputStream inputStream) throws IOException {
        return this.serde.privateKeys().deserialize(
                FileCopyUtils.copyToString(new InputStreamReader(inputStream))
        );
    }

    @Override
    public void serialize(RSAPrivateKey object, OutputStream outputStream) throws IOException {
        outputStream.write(this.serde.privateKeys().serialize(object).getBytes(StandardCharsets.UTF_8));
    }
}

class RsaPublicKeySerializer implements Serializer<RSAPublicKey>, Deserializer<RSAPrivateKey> {

    private final RsaKeyPairSerde rsaKeyPairSerde = new RsaKeyPairSerde();

    @Override
    public void serialize(RSAPublicKey object, OutputStream outputStream) throws IOException {
        var pem = this.rsaKeyPairSerde.publicKeys().serialize(object);
        FileCopyUtils.copy(pem.getBytes(StandardCharsets.UTF_8), outputStream);
    }

    @Override
    public RSAPrivateKey deserialize(InputStream inputStream) throws IOException {
        return this.rsaKeyPairSerde.privateKeys()
                .deserialize(FileCopyUtils.copyToString(new InputStreamReader(inputStream)));
    }
}
