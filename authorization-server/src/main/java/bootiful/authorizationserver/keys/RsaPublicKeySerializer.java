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
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
class RsaPublicKeySerializer implements Serializer<RSAPublicKey>, Deserializer<RSAPublicKey> {

    @Override
    public void serialize(RSAPublicKey object, OutputStream outputStream) throws IOException {
        var x509EncodedKeySpec = new X509EncodedKeySpec(object.getEncoded());
        var pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(x509EncodedKeySpec.getEncoded()) + "\n-----END PUBLIC KEY-----";
        FileCopyUtils.copy(pem.getBytes(StandardCharsets.UTF_8), outputStream);
    }

    @Override
    public RSAPublicKey deserialize(InputStream inputStream) throws IOException {
        try {
            var pem = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
            var publicKeyPEM = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
            var encoded = Base64.getMimeDecoder().decode(publicKeyPEM);
            var keyFactory = KeyFactory.getInstance("RSA");
            var keySpec = new X509EncodedKeySpec(encoded);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        }//
        catch (Throwable throwable) {
            throw new IllegalArgumentException("there's been an exception", throwable);
        }

    }
}
