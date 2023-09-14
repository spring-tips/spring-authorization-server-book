package bootiful.authorizationserver.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.serializer.Deserializer;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.function.Function;

class KeyConfigurationTest {

    private final String keyId = "test";

    private final KeyConfiguration keyConfiguration =
            new KeyConfiguration(this.keyId);

    private final RsaKeyPairRepository.RsaKeyPair keyPair =
            this.keyConfiguration.generateKeyPair(Instant.now());

    private final TextEncryptor encryptor = Encryptors.delux("1233", "a32e");
    private final Charset charset = Charset.defaultCharset();

    @Test
    void privateKeyPlain() throws Exception {
        this.doPlainTest(this.keyPair.privateKey(), new RsaPrivateKeyConverter(textEncryptor),
                this::toString);
    }

    @Test
    void publicKeyPlain() throws Exception {
        this.doPlainTest(this.keyPair.publicKey(), new RsaPublicKeyConverter(textEncryptor),
                this::toString);
    }

    @Test
    void publicKeyEncrypted() throws Exception {
        this.doEncryptedTest(this.keyPair.publicKey(), this::toString);
    }


    @Test
    void privateKeyEncrypted() throws Exception {
        this.doEncryptedTest(this.keyPair.privateKey(), this::toString);
    }

    private String toString(RSAPrivateKey privateKey) {
        try {
            var converter = new RsaPrivateKeyConverter(textEncryptor);
            try (var baos = new ByteArrayOutputStream()) {
                converter.serialize(privateKey, baos);
                return baos.toString(this.charset);
            }
        }//
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private String toString(RSAPublicKey publicKey) {
        try {
            var converter = new RsaPublicKeyConverter(textEncryptor);
            try (var baos = new ByteArrayOutputStream()) {
                converter.serialize(publicKey, baos);
                return baos.toString(this.charset);
            }
        }//
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private <T> void doPlainTest(T t, Deserializer<T> deserializer,
                                 Function<T, String> function) throws Exception {
        var string = function.apply(t);
        var privateKey = deserializer.deserialize(new ByteArrayInputStream(string.getBytes()));
        Assertions.assertEquals(privateKey, t);
    }

    private <T> void doEncryptedTest(T t, Function<T, String> toStringFunction)
            throws Exception {
        var input = toStringFunction.apply(t);
        var baos = new ByteArrayOutputStream();
        try (var eos = new EncryptedOutputStream(baos, this.encryptor)) {
            eos.write(input.getBytes());
        }
        var bais = new ByteArrayInputStream(baos.toByteArray());
        try (var eis = new EncryptedInputStream(bais, this.encryptor)) {
            var readData = new byte[input.length()];
            eis.read(readData);
            Assertions.assertEquals(new String(readData), input);
        }
    }


}