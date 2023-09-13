package bootiful.authorizationserver.keys;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
class RsaPrivateKeySerializer implements Serializer<RSAPrivateKey>,
        Deserializer<RSAPrivateKey> {

    @Override
    public RSAPrivateKey deserialize(InputStream inputStream) throws IOException {

        try {
            var pem = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
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

    @Override
    public void serialize(RSAPrivateKey object, OutputStream outputStream) throws IOException {
        var pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(object.getEncoded());
        outputStream.write(("-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded()) + "\n-----END PRIVATE KEY-----").getBytes(StandardCharsets.UTF_8));
    }
}