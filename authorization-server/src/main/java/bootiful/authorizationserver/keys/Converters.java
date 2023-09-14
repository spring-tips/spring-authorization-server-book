package bootiful.authorizationserver.keys;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
class Converters {

    private final RsaPublicKeyConverter publicKeyConverter =
            new RsaPublicKeyConverter();

    private final RsaPrivateKeyConverter rsaPrivateKeyConverter =
            new RsaPrivateKeyConverter();

    @Bean
    Deserializer<RSAPublicKey> rsaPublicKeyDeserializer() {
        return this.publicKeyConverter;
    }

    @Bean
    Serializer<RSAPublicKey> rsaPublicKeySerializer() {
        return this.publicKeyConverter;
    }

    @Bean
    Serializer<RSAPrivateKey> rsaPrivateKeySerializer() {
        return this.rsaPrivateKeyConverter;
    }

    @Bean
    Deserializer<RSAPrivateKey> rsaPrivateKeyDeserializer() {
        return this.rsaPrivateKeyConverter;
    }
}
