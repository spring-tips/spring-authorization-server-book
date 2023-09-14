package bootiful.authorizationserver.keys;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


class EncryptedOutputStream extends OutputStream {

    private final OutputStream target;

    private final TextEncryptor textEncryptor;

    private final ByteArrayOutputStream buffer;

    public EncryptedOutputStream(OutputStream target, TextEncryptor textEncryptor) {
        this.target = target;
        this.textEncryptor = textEncryptor;
        this.buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) {
        buffer.write(b);
    }

    @Override
    public void close() throws IOException {
        this.buffer.flush();
        var encryptedData = this.textEncryptor.encrypt(this.buffer.toString());
        target.write(encryptedData.getBytes());
        buffer.reset();
        target.close();
    }
}