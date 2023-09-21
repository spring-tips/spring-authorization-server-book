package bootiful.authorizationserver.keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
class LifecycleConfiguration {

    // <.>
    @Bean
    ApplicationListener<RsaKeyPairGenerationRequestEvent> keyPairGenerationRequestListener(
            Keys keys, RsaKeyPairRepository repository, @Value("${jwk.key.id}") String keyId) {
        return event -> repository.save(keys.generateKeyPair(keyId, event.getSource()));
    }

    // <.>
    @Bean
    ApplicationListener<ApplicationReadyEvent> applicationReadyListener(
            ApplicationEventPublisher publisher, RsaKeyPairRepository repository) {
        return event -> {
            if (repository.findKeyPairs().isEmpty())
                publisher.publishEvent(new RsaKeyPairGenerationRequestEvent(Instant.now()));
        };
    }
}
