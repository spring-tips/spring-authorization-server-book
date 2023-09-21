package bootiful.authorizationserver.keys;

import org.springframework.core.serializer.Deserializer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component
class RsaKeyPairRowMapper implements RowMapper<RsaKeyPair> {

    private final RsaPrivateKeyConverter rsaPrivateKeyConverter;

    private final RsaPublicKeyConverter rsaPublicKeyConverter;

    RsaKeyPairRowMapper(RsaPrivateKeyConverter rsaPrivateKeyConverter,
                        RsaPublicKeyConverter rsaPublicKeyConverter) {
        this.rsaPrivateKeyConverter = rsaPrivateKeyConverter;
        this.rsaPublicKeyConverter = rsaPublicKeyConverter;
    }

    @Override
    public RsaKeyPair mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {

            // <.>
            var privateKey = loadKey(rs, "private_key", this.rsaPrivateKeyConverter);
            var publicKey = loadKey(rs, "public_key", this.rsaPublicKeyConverter);

            // <.>
            var created = new Date(rs.getDate("created").getTime()).toInstant();
            var id = rs.getString("id");

            // <.>
            return new RsaKeyPair(id, created, publicKey, privateKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T loadKey(ResultSet rs, String fn, Deserializer<T> f)
            throws SQLException, IOException {
        var privateKeyBytes = rs.getString(fn).getBytes();
        return f.deserializeFromByteArray(privateKeyBytes);
    }
}
