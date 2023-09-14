package bootiful.authorizationserver.keys;


import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

class EncryptedInputStream extends InputStream {

    private final InputStream source;
    private final TextEncryptor textEncryptor;
    private final AtomicReference<ByteArrayInputStream> decryptedStream = new AtomicReference<>();

    EncryptedInputStream(InputStream source, TextEncryptor textEncryptor) {
        this.source = source;
        this.textEncryptor = textEncryptor;
    }

    private void ensureDecryption() throws IOException {

        if (this.decryptedStream.get() != null)
            return;

        try (var baos = new ByteArrayOutputStream()) {
            var buffer = new byte[1024];
            var bytesRead = -1;
            while ((bytesRead = source.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            baos.flush();
            var decryptedText = textEncryptor.decrypt(new String (baos.toByteArray()));
            this.decryptedStream.set(new ByteArrayInputStream(decryptedText.getBytes()));
        }
    }

    @Override
    public int read() throws IOException {
        this.ensureDecryption();
        return this.decryptedStream.get().read();
    }
}