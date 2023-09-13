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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.util.Assert;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
    OAuth2TokenGenerator<OAuth2Token> delegatingOAuth2TokenGenerator(JwtEncoder encoder, OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var jg = this.jwtGenerator(encoder, customizer);
        return new DelegatingOAuth2TokenGenerator(jg, new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator());
    }

    JwtGenerator jwtGenerator(JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        var generator = new JwtGenerator(jwtEncoder);
        generator.setJwtCustomizer(customizer);
        return generator;
    }


    /*
    @Bean
    TextEncryptor textEncryptor(
        @Value("${jwk.persistence.password}") String password,
        @Value("${jwk.persistence.salt}") String salt) {
        return Encryptors.noOpText() ;///  Encryptors.text(password, salt);
    }
    */

    /*  @Bean
      JdbcRsaKeyPairRepository rsaKeyPairRepository(JdbcTemplate template) {
          return new JdbcRsaKeyPairRepository(Encryptors.noOpText(),
                  template);
      }
  */
    KeyConfiguration(@Value("${jwk.key.id}") String keyId,
                     ApplicationEventPublisher publisher) {
        this.keyId = keyId;
        this.publisher = publisher;
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

    @Bean
    InMemoryRsaKeyPairRepository inMemoryRsaKeyPairRepository() {
        return new InMemoryRsaKeyPairRepository();
    }

    @Bean
    ApplicationListener<KeyPairGenerationRequestEvent> keyPairGenerationRequestListener(RsaKeyPairRepository repository) {
        return event -> repository.save(generateKeyPair(event.getSource()));
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> applicationReadyListener(RsaKeyPairRepository repository) {
        return event -> {
            if (repository.findKeyPairs().isEmpty())
                this.publisher.publishEvent(new KeyPairGenerationRequestEvent(Instant.now()));
        };
    }
}


class JdbcRsaKeyPairRepository implements RsaKeyPairRepository {

    private final JdbcTemplate template;

    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    // todo fix this!
    private final TextEncryptor textEncryptor;

    JdbcRsaKeyPairRepository(TextEncryptor textEncryptor, JdbcTemplate template) {
        this.template = template;
        this.textEncryptor = textEncryptor;
    }

    class RsaKeyPairRowMapper implements RowMapper<RsaKeyPair> {


        @Override
        public RsaKeyPair mapRow(ResultSet rs, int rowNum) throws SQLException {
            var publicKey = read(rs, "public_key", (Function<byte[], RSAPublicKey>) JdbcRsaKeyPairRepository::deserialize);
            var privateKey = read(rs, "private_key", (Function<byte[], RSAPrivateKey>) JdbcRsaKeyPairRepository::deserialize);
            return new RsaKeyPair(
                    rs.getString("id"),
                    new Date(rs.getDate("created").getTime()).toInstant(),
                    publicKey, privateKey);
        }

        private <T> T read(ResultSet rs, String colName, Function<byte[], T> function) throws SQLException {
            var base64EncodedString = base64Decoder.decode(textEncryptor.decrypt((rs.getString(colName))));
            return function.apply(base64EncodedString);
        }
    }

    @Override
    public List<RsaKeyPair> findKeyPairs() {
        return this.template.query("select * from rsa_key_pairs order by created desc",
                new RsaKeyPairRowMapper());
    }

    @Override
    public void delete(String id) {
        var updated = this.template.update("delete from rsa_key_pairs where id =?", id);
        Assert.state(updated == 0 || updated == 1, "no more than one row should've been affected");
    }

    private <T> String write(T t) {
        var bytes = serialize(t);
        var string = this.base64Encoder.encodeToString(bytes);
        return this.textEncryptor.encrypt(string);
    }

    private static byte[] serialize(Object o) {
        var baos = new ByteArrayOutputStream();
        Assert.state(o instanceof Serializable, "the object must be Serializable!");
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(o);
            oos.flush();
        }//
        catch (Throwable t) {
            throw new RuntimeException("can not serialize the object ", t);
        }//

        return baos.toByteArray();

    }

    private static <T> T deserialize(byte[] input) {
        try (var byteArrayInputStream = new ObjectInputStream(
                new ByteArrayInputStream(input))) {
            return (T) byteArrayInputStream.readObject();
        }//
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private <T> T read(String string) {
        var decryptedString = this.textEncryptor.decrypt(string);
        var decodedString = this.base64Decoder.decode(decryptedString);
        return deserialize(decodedString);
    }

    @Override
    public void save(RsaKeyPair keyPair) {
        var sql = """
                INSERT INTO rsa_key_pairs (id, private_key, public_key, created)
                VALUES (?, ?, ?, ?)
                on conflict  on constraint rsa_key_pairs_id_created_key
                do nothing
                """;
        var publicKey = write(keyPair.publicKey());
        var privateKey = write(keyPair.privateKey());
        var updated = this.template.update(sql, keyPair.id(), privateKey,
                publicKey, new Date(keyPair.created().toEpochMilli()));
        Assert.state(updated == 0 || updated == 1, "no more than one record should have been updated");
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


class KeyPairGenerationRequestEvent extends ApplicationEvent {

    public KeyPairGenerationRequestEvent(Instant instant) {
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

