package bootiful.authorizationserver.keys;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;

@Component
class RsaKeyPairEncryptingBeanPostProcessor
        implements BeanPostProcessor {

    private final TextEncryptor encryptor;

    RsaKeyPairEncryptingBeanPostProcessor(TextEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    private static class EncryptingConverter<T extends Key>
            implements Serializer<T>, Deserializer<T> {

        private final TextEncryptor textEncryptor;
        private final Serializer<T> targetSerializer;

        private final Deserializer<T> targetDeserializer;

        private EncryptingConverter(TextEncryptor textEncryptor, Serializer<T> targetSerializer, Deserializer<T> targetDeserializer) {
            this.textEncryptor = textEncryptor;
            this.targetSerializer = targetSerializer;
            this.targetDeserializer = targetDeserializer;
        }

        @Override
        public void serialize(T object, OutputStream outputStream) throws IOException {
            this.targetSerializer.serialize(object, new EncryptedOutputStream(outputStream, this.textEncryptor));
        }

        @Override
        public T deserialize(InputStream inputStream) throws IOException {
            try {
                return this.targetDeserializer.deserialize(
                        new EncryptedInputStream(inputStream, this.textEncryptor));
            }//
            catch (Exception e) {
                throw new IllegalStateException("the state is invalid", e);
            }
        }

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof RsaPublicKeyConverter target) {
            return new EncryptingConverter<>(this.encryptor, target, target);
        }

        if (bean instanceof RsaPrivateKeyConverter target) {
            return new EncryptingConverter<>(this.encryptor, target, target);
        }

        return bean;
    }
}
