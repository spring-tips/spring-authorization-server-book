package bootiful.authorizationserver.keys;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component
class RsaKeyPairRowMapper implements RowMapper<RsaKeyPairRepository.RsaKeyPair> {

    private final RsaPrivateKeySerializer privateKeySerializer;

    private final RsaPublicKeySerializer publicKeySerializer;

    RsaKeyPairRowMapper(RsaPrivateKeySerializer privateKeySerializer, RsaPublicKeySerializer publicKeySerializer) {
        this.privateKeySerializer = privateKeySerializer;
        this.publicKeySerializer = publicKeySerializer;
    }

    @Override
    public RsaKeyPairRepository.RsaKeyPair mapRow(ResultSet rs, int rowNum) throws SQLException {

        try {
            var privateKey = this.privateKeySerializer.deserializeFromByteArray(
                    rs.getString("private_key").getBytes()
            );
            var publicKey = this.publicKeySerializer.deserializeFromByteArray(
                    rs.getString("public_key").getBytes()
            );
            return new RsaKeyPairRepository.RsaKeyPair(rs.getString("id"),
                    new Date(rs.getDate("created").getTime()).toInstant(),
                    publicKey, privateKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
